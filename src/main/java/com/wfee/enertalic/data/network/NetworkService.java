package com.wfee.enertalic.data.network;

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

public class NetworkService {
    private final static HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static NetworkService instance;

    private final List<EnergyGroupNetwork> networks;
    private final List<EnergyGroup> groups;

    private NetworkService() {
        this.networks = new ArrayList<>();
        this.groups = new ArrayList<>();
    }

    public static NetworkService getInstance() {
        if (instance == null) {
            instance = new NetworkService();
        }

        return instance;
    }

    public void tick(double dt) {
        networks.forEach(network -> network.computeNetwork(dt));
    }

    public void addNewObject(EnergyObject object, int x, int y, int z, World world) {
        world.execute(() -> groups.add(analyzeGroup(object, x, y, z, world)));
    }

    public void removeObject(EnergyObject object) {
        EnergyGroup existingGroup = groups.stream()
                .filter(group -> group.contains(object))
                .findAny()
                .orElse(null);

        if (existingGroup == null) {
            return;
        }

        EnergyGroupNetwork network = existingGroup.getNetwork();

        if (network != null) {
            networks.remove(network);
        }

        if (!existingGroup.remove(object)) {
            throw new IllegalStateException("Object must be in a group");
        }

        network = createNewNetwork(existingGroup);
        existingGroup.setNetwork(network);
        networks.add(network);
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
        EnergyGroup existingGroup = analyzeSides(start, position, world, seenPositions, group);

        if (existingGroup != null) {
            networks.remove(existingGroup.getNetwork());
        }

        EnergyGroupNetwork newNetwork = createNewNetwork(group);

        if (newNetwork != null) {
            group.setNetwork(newNetwork);
            networks.add(newNetwork);
        }

        return group;
    }

    public void addToGroup(
            Vector3i position,
            World world,
            EnergyGroup group
    ) {
        Holder<ChunkStore> holder = world.getBlockComponentHolder(position.x, position.y, position.z);

        if (holder == null) {
            return;
        }

        EnergyNode node = holder.getComponent(EnergyNode.getComponentType());

        if (node != null) {
            for (Direction direction : Direction.values()) {
                EnergyConfig config = node.getEnergySideConfig().getDirection(direction);

                if (config.canExport()) {
                    group.getProviders().add(new AnalyzedEnergyObject(node, position));
                } else if (config.canImport()) {
                    group.getConsumers().add(new AnalyzedEnergyObject(node, position));
                }
            }
        }

        EnergyTransfer transfer = holder.getComponent(EnergyTransfer.getComponentType());

        if (transfer != null) {
            group.getTransfers().add(new AnalyzedEnergyObject(transfer, position));
        }
    }

    public EnergyGroup analyzeSides(
            EnergyObject last,
            Vector3i position,
            World world,
            Set<Vector3i> seenPositions,
            EnergyGroup group
    ) {
        EnergyGroup existingGroup = null;

        for (Direction direction : Direction.values()) {
            if (last.getEnergySideConfig().getDirection(direction) != EnergyConfig.OFF) {
                EnergyGroup analyzedGroup = analyzePosition(
                        position.clone().add(direction.getOffset()),
                        world,
                        seenPositions,
                        group
                );

                if (analyzedGroup != null) {
                    existingGroup = analyzedGroup;
                }
            }
        }

        return existingGroup;
    }

    private EnergyGroup analyzePosition(
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
        EnergyGroup oldGroup = analyzeSides(object, position, world, seenPositions, group);

        if (oldGroup != null) {
            return oldGroup;
        }

        Optional<EnergyGroup> existingGroup = groups
                .stream()
                .filter(savedGroup -> savedGroup.contains(position))
                .findAny();

        return existingGroup.orElse(null);
    }
}
