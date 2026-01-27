package com.wfee.enertalic.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktick.BlockTickStrategy;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.ChunkSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.wfee.enertalic.components.EnergyNode;
import com.wfee.enertalic.components.EnergyObject;
import com.wfee.enertalic.components.EnergyTransfer;
import com.wfee.enertalic.data.EnergyConfig;
import com.wfee.enertalic.util.AnalyzedEnergyObject;
import com.wfee.enertalic.util.Direction;
import com.wfee.enertalic.util.EnergyGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

public class EnergyTickSystem extends EntityTickingSystem<ChunkStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final Query<ChunkStore> query;
    private final Map<EnergyNode, GroupNetwork> networks;

    public EnergyTickSystem() {
        this.query = Query.and(BlockSection.getComponentType(), ChunkSection.getComponentType());
        this.networks = new HashMap<>();
    }

    @Override
    public void tick(
            float dt,
            int index,
            @Nonnull ArchetypeChunk<ChunkStore> archetypeChunk,
            @Nonnull Store<ChunkStore> store,
            @Nonnull CommandBuffer<ChunkStore> commandBuffer
    ) {
        BlockSection blocks = archetypeChunk.getComponent(index, BlockSection.getComponentType());

        assert blocks != null;

        if (blocks.getTickingBlocksCountCopy() != 0) {
            ChunkSection section = archetypeChunk.getComponent(index, ChunkSection.getComponentType());

            assert section != null;

            BlockComponentChunk blockComponentChunk = commandBuffer.getComponent(
                    section.getChunkColumnReference(),
                    BlockComponentChunk.getComponentType()
            );

            assert blockComponentChunk != null;

            blocks.forEachTicking(
                    blockComponentChunk,
                    commandBuffer,
                    section.getY(),
                    (
                            blockComponentChunk1,
                            commandBuffer1,
                            localX,
                            localY,
                            localZ,
                            _
                    ) ->
                    {
                        Ref<ChunkStore> blockRef = blockComponentChunk1.getEntityReference(
                                ChunkUtil.indexBlockInColumn(localX, localY, localZ)
                        );

                        if (blockRef == null) {
                            return BlockTickStrategy.IGNORED;
                        }

                        EnergyNode node = commandBuffer1.getComponent(blockRef, EnergyNode.getComponentType());

                        if (node == null) {
                            return BlockTickStrategy.IGNORED;
                        }

                        if (node.getCurrentEnergy() == 0) {
                            return BlockTickStrategy.CONTINUE;
                        }

                        WorldChunk chunk = commandBuffer.getComponent(
                                section.getChunkColumnReference(),
                                WorldChunk.getComponentType()
                        );

                        if (chunk == null) {
                            return BlockTickStrategy.IGNORED;
                        }

                        World world = chunk.getWorld();

                        if (world == null) {
                            return BlockTickStrategy.IGNORED;
                        }

                        GroupNetwork nodeNetwork = networks.get(node);

                        if (nodeNetwork == null) {
                            nodeNetwork = networks
                                    .values()
                                    .stream()
                                    .filter(network -> network.network
                                            .keySet()
                                            .stream()
                                            .anyMatch(block -> node.equals(block.getEnergyObject()))
                                    )
                                    .findAny()
                                    .orElse(null);
                            if (nodeNetwork == null) {
                                EnergyGroup group = analyzeGroup(
                                        node,
                                        localX + chunk.getX() * 32,
                                        localY,
                                        localZ + chunk.getZ() * 32,
                                        world
                                );

                                GroupNetwork network = createNewNetwork(group);

                                if (network != null)
                                {
                                    networks.put(node, network);
                                }
                            }
                        }

                        return BlockTickStrategy.CONTINUE;
                    }
            );

            computeNetworks(dt);
        }
    }

    private long getTransferRate(GroupNetwork network, double dt, Edge edge) {
        return (long)(network.network
                .get(edge.destination)
                .stream()
                .filter(extremityEdge -> extremityEdge.destination != edge.source)
                .mapToLong(extremityEdge -> extremityEdge.used)
                .sum() * dt);
    }

    private void computeNetworks(double dt) {
        for (GroupNetwork network : networks.values()) {
            checkAndRebalanceNetwork(network, dt);

            List<Edge> startEdges = network.network.get(GroupNetwork.START);

            for (Edge edge : startEdges) {
                long transfer = getTransferRate(network, dt, edge);

                if (!(edge.destination.getEnergyObject() instanceof EnergyNode node)) {
                    throw new IllegalArgumentException(edge.destination.getEnergyObject().getClass().getName());
                }

                node.removeEnergy(transfer);
            }

            List<Edge> endEdges = network.network.get(GroupNetwork.END);

            for (Edge edge : endEdges) {
                // Needs to be negated because the edge is the reverse
                long transfer = -getTransferRate(network, dt, edge);

                if (!(edge.destination.getEnergyObject() instanceof EnergyNode node)) {
                    throw new IllegalArgumentException(edge.destination.getEnergyObject().getClass().getName());
                }

                node.addEnergy(transfer);
            }
        }
    }

    private void checkAndRebalanceNetwork(GroupNetwork network, double dt) {
        List<Edge> startEdges = network.network.get(GroupNetwork.START);
        boolean needsRebalancing = false;

        for (Edge edge : startEdges) {
            long transfer = getTransferRate(network, dt, edge);

            if (!(edge.destination.getEnergyObject() instanceof EnergyNode node)) {
                throw new IllegalArgumentException(edge.destination.getEnergyObject().getClass().getName());
            }

            if (node.getCurrentEnergy() < transfer) {
                needsRebalancing = true;
                edge.capacity = Math.round(node.getCurrentEnergy() / dt);
            }
        }

        List<Edge> endEdges = network.network.get(GroupNetwork.END);

        for (Edge edge : endEdges) {
            // Needs to be negated because the edge is the reverse
            long transfer = -getTransferRate(network, dt, edge);

            if (!(edge.destination.getEnergyObject() instanceof EnergyNode node)) {
                throw new IllegalArgumentException(edge.destination.getEnergyObject().getClass().getName());
            }

            if (node.getEnergyRemaining() < transfer) {
                needsRebalancing = true;
                edge.capacity = Math.round(node.getEnergyRemaining() / dt);
            }
        }

        if (needsRebalancing) {
            network.network.values().stream().flatMap(List::stream).forEach(edge -> edge.used = 0);
            findBestPath(network);
        }
    }

    private GroupNetwork createNewNetwork(EnergyGroup group) {
        if (group.providers().isEmpty() || group.consumers().isEmpty())
        {
            return null;
        }

        GroupNetwork network = new GroupNetwork(group);
        findBestPath(network);
        return network;
    }

    private void findBestPath(GroupNetwork network) {
        List<PathPart> path = getPath(GroupNetwork.START, GroupNetwork.END, network, new ArrayList<>());

        while (path != null) {
            long flow = getBottleneckFlow(path);

            for (PathPart pathPart : path) {
                pathPart.edge.used += flow;
                pathPart.edge.reverse.used -= flow;
                path = getPath(GroupNetwork.START, GroupNetwork.END, network, new ArrayList<>());
            }
        }
    }

    private long getBottleneckFlow(List<PathPart> path) {
        return path
                .stream()
                .map(pathPart -> pathPart.residualEnergy)
                .min(Long::compare)
                .orElse(Long.MAX_VALUE);
    }

    private List<PathPart> getPath(AnalyzedEnergyObject start, AnalyzedEnergyObject end, GroupNetwork network, List<PathPart> path) {
        if (start == end) {
            return path;
        }

        for (Edge edge : network.getEdges(start)) {
            long residualCapacity = edge.capacity - edge.used;

            PathPart pathPart = new PathPart(edge, residualCapacity);

            if (residualCapacity > 0 && !path.contains(pathPart)) {
                path.add(pathPart);

                List<PathPart> result = getPath(edge.destination, end, network, path);

                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    private EnergyGroup analyzeGroup(EnergyNode start, int x, int y, int z, World world) {
        Vector3i position = new Vector3i(x, y, z);
        EnergyGroup group = new EnergyGroup(new HashSet<>(), new HashSet<>(), new HashSet<>());
        Set<Vector3i> seenPositions = new HashSet<>() {
            {
                add(position);
            }
        };

        addToGroup(position, world, group);
        analyzeSides(start, position, world, group, seenPositions);
        return group;
    }

    private void addToGroup(
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
                    group.providers().add(new AnalyzedEnergyObject(node, position));
                } else if (config.canImport()) {
                    group.consumers().add(new AnalyzedEnergyObject(node, position));
                }
            }
        }

        EnergyTransfer transfer = holder.getComponent(EnergyTransfer.getComponentType());

        if (transfer != null) {
            group.transfers().add(new AnalyzedEnergyObject(transfer, position));
        }
    }

    private void analyzeSides(
            EnergyObject last,
            Vector3i position,
            World world,
            EnergyGroup group,
            Set<Vector3i> seenPositions
    ) {
        for (Direction direction : Direction.values()) {
            if (last.getEnergySideConfig().getDirection(direction) != EnergyConfig.OFF) {
                analyzePosition(
                        position.clone().add(direction.getOffset()),
                        world,
                        group,
                        seenPositions
                );
            }
        }
    }

    private void analyzePosition(
            Vector3i position,
            World world,
            EnergyGroup group,
            Set<Vector3i> seenPositions
    ) {

        if (!seenPositions.add(position)) {
            return;
        }

        Holder<ChunkStore> holder = world.getBlockComponentHolder(position.x, position.y, position.z);

        if (holder == null) {
            return;
        }

        EnergyObject object = holder.getComponent(EnergyNode.getComponentType());

        if (object == null) {
            object = holder.getComponent(EnergyTransfer.getComponentType());
        }

        if (object != null) {
            addToGroup(position, world, group);
            analyzeSides(object, position, world, group, seenPositions);
        }
    }

    @Nullable
    @Override
    public Query<ChunkStore> getQuery() {
        return query;
    }

    public static final class Edge {
        private final AnalyzedEnergyObject source;
        private final AnalyzedEnergyObject destination;
        private long capacity;
        private final List<AnalyzedEnergyObject> affects;
        private Edge reverse;
        private long used;

        public Edge(AnalyzedEnergyObject source,
                    AnalyzedEnergyObject destination,
                    List<AnalyzedEnergyObject> affects,
                    long capacity) {
            this.source = source;
            this.destination = destination;
            this.capacity = capacity;
            this.affects = affects;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            return obj instanceof Edge that &&
                    this.source == that.source &&
                    this.destination == that.destination &&
                    this.capacity == that.capacity &&
                    this.used == that.used;
        }
    }

    public record PathPart(Edge edge, long residualEnergy) {
    }

    public static class GroupNetwork {
        private final Map<AnalyzedEnergyObject, List<Edge>> network;

        private static final AnalyzedEnergyObject START = new AnalyzedEnergyObject(new EnergyNode(), null);
        private static final AnalyzedEnergyObject END = new AnalyzedEnergyObject(new EnergyNode(), null);

        public GroupNetwork(EnergyGroup group) {
            this.network = new HashMap<>();
            traverseGroup(group);
        }

        private void traverseGroup(EnergyGroup group) {
            for (AnalyzedEnergyObject source : group.providers())
            {
                addSuperEdge(START, source);
                Set<Vector3i> visitedPositions = new HashSet<>();

                traverseGroupFromNode(source, group, visitedPositions, null);
            }

            for (AnalyzedEnergyObject destination : group.consumers())
            {
                addSuperEdge(destination, END);
            }
        }

        private void traverseGroupFromNode(AnalyzedEnergyObject object, EnergyGroup group, Set<Vector3i> visitedPositions, @Nullable List<AnalyzedEnergyObject> currentEdge) {
            if (!visitedPositions.add(object.getPosition())) {
                return;
            }

            if (object.getEnergyObject() instanceof EnergyNode && currentEdge != null && !currentEdge.isEmpty()) {
                List<AnalyzedEnergyObject> transfers = currentEdge.subList(1, currentEdge.size());

                addSuperEdge(currentEdge.getFirst(), object, transfers, transfers
                        .stream()
                        .map(transfer -> ((EnergyTransfer)transfer.getEnergyObject()).getMaxTransferRate())
                        .min(Long::compare)
                        .orElse(Long.MAX_VALUE)
                );

                currentEdge = null;
            }

            if (currentEdge != null) {
                currentEdge.add(object);
            }

            for (AnalyzedEnergyObject next : getSurroundingObjects(object, group)) {
                if (object.getEnergyObject() instanceof EnergyNode) {
                    currentEdge = new ArrayList<>() {{ add(object); }};
                }

                traverseGroupFromNode(next, group, visitedPositions, currentEdge);
            }
        }

        private List<AnalyzedEnergyObject> getSurroundingObjects(AnalyzedEnergyObject object, EnergyGroup group) {
            List<AnalyzedEnergyObject> surroundingObjects = new ArrayList<>();

            for (Direction direction : Direction.values()) {
                if (!object.getEnergyObject().getEnergySideConfig().getDirection(direction).canExport()) {
                    continue;
                }

                Vector3i searchPosition = object.getPosition().clone().add(direction.getOffset());

                Consumer<Set<AnalyzedEnergyObject>> getMatchingObject = objects -> objects.stream()
                        .filter(objectPosition -> objectPosition.getPosition().equals(searchPosition))
                        .filter(energyObject ->
                                energyObject
                                        .getEnergyObject()
                                        .getEnergySideConfig()
                                        .getDirection(direction.getOpposite())
                                        .canImport()
                        )
                        .findAny()
                        .ifPresent(surroundingObjects::add);

                getMatchingObject.accept(group.consumers());
                getMatchingObject.accept(group.transfers());
            }

            return surroundingObjects;
        }

        private void addSuperEdge(AnalyzedEnergyObject source, AnalyzedEnergyObject destination) {
            addSuperEdge(source, destination, new ArrayList<>(), Long.MAX_VALUE);
        }

        private void addSuperEdge(AnalyzedEnergyObject source,
                                  AnalyzedEnergyObject destination,
                                  List<AnalyzedEnergyObject> affects,
                                  long capacity) {
            Edge edge = new Edge(source, destination, affects, capacity);
            Edge reverse = new Edge(destination, source, affects, 0);
            edge.reverse = reverse;
            reverse.reverse = edge;
            network.putIfAbsent(source, new ArrayList<>());
            network.putIfAbsent(destination, new ArrayList<>());
            network.get(source).add(edge);
            network.get(destination).add(reverse);
        }

        public List<Edge> getEdges(AnalyzedEnergyObject source) {
            return network.getOrDefault(source, null);
        }
    }
}
