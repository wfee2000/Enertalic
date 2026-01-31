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

    public void addNewObject(EnergyObject object, int x, int y, int z, World world, AddReason addReason) {
        world.execute(() -> {
            if (addReason == AddReason.LOAD && groups
                    .stream()
                    .anyMatch(group -> group.contains(new Vector3i(x,y,z)))) {
                return;
            }

            groups.add(analyzeGroup(object, x, y, z, world));
        });
    }

    public void removeObject(Vector3i position) {
        EnergyGroup existingGroup = groups.stream()
                .filter(group -> group.contains(position))
                .findAny()
                .orElseThrow(() -> new IllegalStateException("Object must be in a group"));

        if (!existingGroup.remove(position)) {
            throw new IllegalStateException("Object must be in a group");
        }

        List<EnergyGroup> newGroups = existingGroup.checkConnections();

        if (existingGroup.isEmpty() || newGroups != null) {
            groups.remove(existingGroup);
        }

        EnergyGroupNetwork network = existingGroup.getNetwork();
        boolean hasNetwork = network != null;

        if (newGroups != null) {
            groups.addAll(newGroups);
        }

        if (hasNetwork) {
            if (!networks.remove(network)) {
                throw new IllegalStateException("Object must be in a Network");
            }

            if (newGroups == null) {
                createAndSetNewNetwork(existingGroup);
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

    private EnergyGroup analyzeGroup(EnergyObject start, int x, int y, int z, World world) {
        Vector3i position = new Vector3i(x, y, z);
        EnergyGroup group = new EnergyGroup(new HashSet<>(), new HashSet<>(), new HashSet<>());
        Set<Vector3i> seenPositions = new HashSet<>() {
            {
                add(position);
            }
        };

        addToGroup(position, world, group);
        List<EnergyGroup> existingGroups = analyzeSides(start, position, world, seenPositions, group);

        if (!existingGroups.isEmpty()) {
            existingGroups.forEach(existingGroup -> {
                groups.remove(existingGroup);
                networks.remove(existingGroup.getNetwork());
            });
        }

        createAndSetNewNetwork(group);
        return group;
    }

    private void addToGroup(Vector3i position, World world, EnergyGroup group) {
        Holder<ChunkStore> holder = world.getBlockComponentHolder(position.x, position.y, position.z);

        if (holder == null) {
            return;
        }

        EnergyNode node = holder.getComponent(EnergyNode.getComponentType());

        if (node != null) {
            for (Direction direction : Direction.values()) {
                EnergyConfig config = node.getEnergySideConfig().getDirection(direction);

                if (config.canExport()) {
                    group.getProviders().add(new AnalyzedEnergyObject(node, position, false));
                } else if (config.canImport()) {
                    group.getConsumers().add(new AnalyzedEnergyObject(node, position, false));
                }
            }
        }

        EnergyTransfer transfer = holder.getComponent(EnergyTransfer.getComponentType());

        if (transfer != null) {
            group.getTransfers().add(new AnalyzedEnergyObject(transfer, position, false));
        }
    }

    private List<EnergyGroup> analyzeSides(
            EnergyObject last,
            Vector3i position,
            World world,
            Set<Vector3i> seenPositions,
            EnergyGroup group
    ) {
        List<EnergyGroup> existingGroups = new ArrayList<>();

        for (Direction direction : Direction.values()) {
            if (last.getEnergySideConfig().getDirection(direction) != EnergyConfig.OFF) {
                List<EnergyGroup> analyzedGroups = analyzePosition(
                        position.clone().add(direction.getOffset()),
                        world,
                        seenPositions,
                        group
                );

                if (analyzedGroups != null) {
                    existingGroups.addAll(analyzedGroups);
                }
            }
        }

        return existingGroups;
    }

    private List<EnergyGroup> analyzePosition(
            Vector3i position,
            World world,
            Set<Vector3i> seenPositions,
            EnergyGroup group
    ) {
        if (!seenPositions.add(position)) {
            return null;
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

        addToGroup(position, world, group);
        List<EnergyGroup> oldGroups = analyzeSides(object, position, world, seenPositions, group);

        if (!oldGroups.isEmpty()) {
            return oldGroups;
        }

        return groups
                .stream()
                .filter(savedGroup -> savedGroup.contains(position))
                .toList();
    }
}
