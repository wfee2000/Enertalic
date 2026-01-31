package com.wfee.enertalic.data.network;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.wfee.enertalic.components.EnergyNode;
import com.wfee.enertalic.components.EnergyObject;
import com.wfee.enertalic.components.EnergyTransfer;
import com.wfee.enertalic.data.EnergyConfig;
import com.wfee.enertalic.util.AnalyzedEnergyObject;
import com.wfee.enertalic.util.Direction;
import com.wfee.enertalic.util.EnergyGroup;
import com.wfee.enertalic.util.Pair;

import java.util.*;

public class EnergyService {
    private final static HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static EnergyService instance;

    private final List<EnergyGroupNetwork> networks;
    private final List<EnergyGroup> groups;

    private EnergyService() {
        this.networks = new ArrayList<>();
        this.groups = new ArrayList<>();
    }

    public static EnergyService getInstance() {
        if (instance == null) {
            instance = new EnergyService();
        }

        return instance;
    }

    public void tick(double dt) {
        networks.forEach(network -> network.computeNetwork(dt));
    }

    public void addNewObject(int x, int y, int z, World world, AddReason addReason) {
        world.execute(() -> {
            if (addReason == AddReason.LOAD && groups
                    .stream()
                    .anyMatch(group -> group.contains(new Vector3i(x,y,z)))) {
                return;
            }

            groups.add(analyzeGroup(x, y, z, world));
        });
    }

    public void removeObject(Vector3i position) {
        Pair<EnergyGroup, AnalyzedEnergyObject> existing = groups.stream()
                .map(group -> new Pair<>(group, group.getObjectForPosition(position)))
                .filter(pair -> pair.item2() != null)
                .findAny()
                .orElseThrow(() -> new IllegalStateException("Object must be in a group"));

        if (!existing.item1().remove(existing.item2())) {
            throw new IllegalStateException("Object must be in a group");
        }

        List<EnergyGroup> newGroups = existing.item1().checkConnections();

        if (existing.item1().isEmpty() || newGroups != null) {
            groups.remove(existing.item1());
        }

        EnergyGroupNetwork network = existing.item1().getNetwork();
        boolean hasNetwork = network != null;

        if (newGroups != null) {
            groups.addAll(newGroups);
        }

        if (hasNetwork) {
            if (!networks.remove(network)) {
                throw new IllegalStateException("Object must be in a Network");
            }

            if (newGroups == null) {
                createAndSetNewNetwork(existing.item1());
            } else {
                for (EnergyGroup group : newGroups) {
                    createAndSetNewNetwork(group);
                }
            }
        }
    }

    private void createAndSetNewNetwork(EnergyGroup group) {
        EnergyGroupNetwork network = createNewNetwork(group);
        group.setNetwork(network);

        if (network != null) {
            networks.add(network);
        }
    }

    private EnergyGroupNetwork createNewNetwork(EnergyGroup group) {
        if (group.getProviders().isEmpty() || group.getConsumers().isEmpty()) {
            return null;
        }

        EnergyGroupNetwork network = new EnergyGroupNetwork(group);
        network.calculate();
        return network;
    }

    private EnergyGroup analyzeGroup(int x, int y, int z, World world) {
        Vector3i position = new Vector3i(x, y, z);
        EnergyGroup group = new EnergyGroup(new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashMap<>());

        List<EnergyGroup> existingGroups = new ArrayList<>();
        analyzePosition(position, world, new HashSet<>(), group, existingGroups);

        if (!existingGroups.isEmpty()) {
            existingGroups.forEach(existingGroup -> {
                groups.remove(existingGroup);
                networks.remove(existingGroup.getNetwork());
            });
        }

        createAndSetNewNetwork(group);
        return group;
    }

    private AnalyzedEnergyObject addToGroup(Vector3i position, World world, EnergyGroup group) {
        Holder<ChunkStore> holder = world.getBlockComponentHolder(position.x, position.y, position.z);
        AnalyzedEnergyObject object = null;

        if (holder == null) {
            return null;
        }

        EnergyNode node = holder.getComponent(EnergyNode.getComponentType());

        if (node != null) {
            object = new AnalyzedEnergyObject(node, position, false);

            for (Direction direction : Direction.values()) {
                EnergyConfig config = node.getEnergySideConfig().getDirection(direction);

                if (config.canExport()) {
                    group.getProviders().add(object);
                } else if (config.canImport()) {
                    group.getConsumers().add(object);
                }
            }

            group.getSurroundingBlocks().putIfAbsent(object, new ArrayList<>());
        }

        EnergyTransfer transfer = holder.getComponent(EnergyTransfer.getComponentType());

        if (transfer != null) {
            object = new AnalyzedEnergyObject(transfer, position, false);
            group.getTransfers().add(object);
            group.getSurroundingBlocks().putIfAbsent(object, new ArrayList<>());
        }

        return object;
    }

    private List<EnergyGroup> analyzeSides(
            AnalyzedEnergyObject last,
            Vector3i position,
            World world,
            Set<AnalyzedEnergyObject> visited,
            EnergyGroup group
    ) {
        List<EnergyGroup> existingGroups = new ArrayList<>();

        for (Direction direction : Direction.values()) {
            if (last.energyObject().getEnergySideConfig().getDirection(direction) != EnergyConfig.OFF) {
                List<EnergyGroup> analyzedGroups = new ArrayList<>();

                AnalyzedEnergyObject object = analyzePosition(
                        position.clone().add(direction.getOffset()),
                        world,
                        visited,
                        group,
                        analyzedGroups
                );

                if (object != null) {
                    group.getSurroundingBlocks().get(last).add(new Pair<>(direction, object));
                }

                if (!analyzedGroups.isEmpty()) {
                    existingGroups.addAll(analyzedGroups);
                }
            }
        }

        return existingGroups;
    }

    private AnalyzedEnergyObject analyzePosition(
            Vector3i position,
            World world,
            Set<AnalyzedEnergyObject> visited,
            EnergyGroup group,
            List<EnergyGroup> groups
    ) {
        Optional<AnalyzedEnergyObject> analyzedObject = visited
                .stream()
                .filter(object -> object.position().equals(position))
                .findAny();

        if (analyzedObject.isPresent()) {
            return analyzedObject.get();
        }

        Holder<ChunkStore> holder = world.getBlockComponentHolder(position.x, position.y, position.z);

        if (holder == null) {
            return null;
        }

        EnergyObject object = holder.getComponent(EnergyNode.getComponentType());

        if (object == null) {
            object = holder.getComponent(EnergyTransfer.getComponentType());
        }

        if (object == null) {
            return null;
        }

        AnalyzedEnergyObject analyzedEnergyObject = addToGroup(position, world, group);
        visited.add(analyzedEnergyObject);
        List<EnergyGroup> oldGroups = analyzeSides(analyzedEnergyObject, position, world, visited, group);

        if (!oldGroups.isEmpty()) {
            groups.addAll(oldGroups);
        } else {
            groups.addAll(this.groups
                    .stream()
                    .filter(savedGroup -> savedGroup.contains(position))
                    .toList()
            );
        }

        return analyzedEnergyObject;
    }
}
