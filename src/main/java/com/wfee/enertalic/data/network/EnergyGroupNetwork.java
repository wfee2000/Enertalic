package com.wfee.enertalic.data.network;

import com.hypixel.hytale.logger.HytaleLogger;
import com.wfee.enertalic.components.EnergyNode;
import com.wfee.enertalic.components.EnergyTransfer;
import com.wfee.enertalic.data.EnergyConfig;
import com.wfee.enertalic.util.*;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

public class EnergyGroupNetwork {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final Map<AnalyzedEnergyObject, List<Edge>> network;
    private final Set<Edge> sourceEdges;
    private final Set<Edge> sinkEdges;

    private static final AnalyzedEnergyObject START = new AnalyzedEnergyObject(new EnergyNode(), null, false);
    private static final AnalyzedEnergyObject END = new AnalyzedEnergyObject(new EnergyNode(), null, true);

    public EnergyGroupNetwork(EnergyGroup group) {
        this.network = new HashMap<>();
        this.sourceEdges = new HashSet<>();
        this.sinkEdges = new HashSet<>();
        traverseGroup(group);
    }

    private void traverseGroup(EnergyGroup group) {
        for (AnalyzedEnergyObject source : group.getProviders())
        {
            sourceEdges.add(addSuperEdge(START, source));
            traverseGroupFromNode(source, group, null);
        }

        for (AnalyzedEnergyObject destination : group.getConsumers())
        {
            sinkEdges.add(addSuperEdge(destination, END));
        }
    }

    private void traverseGroupFromNode(AnalyzedEnergyObject object, EnergyGroup group, @Nullable List<AnalyzedEnergyObject> currentEdge) {
        List<AnalyzedEnergyObject> surroundingAfter = new ArrayList<>();
        long surrounding = getSurroundingObjects(object, group, surroundingAfter);

        if (object.energyObject() instanceof EnergyNode) {
            if (currentEdge != null && !currentEdge.isEmpty()) {
                addEdge(currentEdge.getFirst(), object, currentEdge, getEdgeCapacity(currentEdge));
                currentEdge = null;
            }
        } else if (object.energyObject() instanceof EnergyTransfer transfer) {
            assert currentEdge != null;

            if (surrounding > 2) {
                addEdge(currentEdge.getFirst(), object, currentEdge, getEdgeCapacity(currentEdge));
                AnalyzedEnergyObject clone = new AnalyzedEnergyObject(object.energyObject(), object.position(), true);

                if (network.get(object).stream().noneMatch(edge -> edge.getDestination().equals(clone))) {
                    addEdge(object,
                            clone,
                            List.of(object),
                            transfer.getMaxTransferRate());
                }

                currentEdge = new ArrayList<>();
                currentEdge.add(clone);
                object = clone;
            } else {
                currentEdge.add(object);
            }
        }

        AnalyzedEnergyObject source = object;

        for (AnalyzedEnergyObject next : surroundingAfter) {
            if (network
                    .entrySet()
                    .stream()
                    .anyMatch(entry ->
                            entry.getKey().equals(source) &&
                                    entry.getValue().stream().anyMatch(edge -> edge.getDestination().equals(next)) ||
                            entry.getKey().equals(next) &&
                                    entry.getValue().stream().anyMatch(edge -> edge.getDestination().equals(source))
                    )
            ) {
                continue;
            }

            if (source.energyObject() instanceof EnergyNode) {
                currentEdge = new ArrayList<>() {{ add(source); }};
            }

            if (currentEdge == null || currentEdge.stream().noneMatch(part -> part.position().equals(next.position()))) {
                traverseGroupFromNode(next, group, currentEdge);
            }
        }
    }

    private long getEdgeCapacity(List<AnalyzedEnergyObject> constructingEdge) {
        return constructingEdge
                .stream()
                .map(energyObject -> {
                    if (energyObject.energyObject() instanceof EnergyNode) {
                        return Long.MAX_VALUE;
                    } else if (energyObject.energyObject() instanceof EnergyTransfer existingTransfer) {
                        return existingTransfer.getMaxTransferRate();
                    }

                    throw new IllegalStateException("energy object is of non existing type");
                })
                .min(Long::compare)
                .orElseThrow(() -> new IllegalStateException("edge needs to have an element"));
    }

    private long getSurroundingObjects(AnalyzedEnergyObject object, EnergyGroup group, List<AnalyzedEnergyObject> surroundingPassthroughObjects) {
        long surrounding = 0;
        for (Pair<Direction, AnalyzedEnergyObject> pair : group.getSurroundingBlocks().get(object)) {
            EnergyConfig config  = object.energyObject().getEnergySideConfig().getDirection(pair.item1());

            surrounding++;

            if (!config.canExport()) {
                continue;
            }

            if (group.getConsumers().contains(pair.item2()) ||
                    group.getTransfers().contains(pair.item2()) ||
                    pair.item2().energyObject().getEnergySideConfig().getDirection(pair.item1().getOpposite()).canImport()) {
                surroundingPassthroughObjects.add(pair.item2());
            }
        }

        return surrounding;
    }

    private Edge addSuperEdge(AnalyzedEnergyObject source, AnalyzedEnergyObject destination) {
        return addEdge(source, destination, new ArrayList<>(), Long.MAX_VALUE);
    }

    private Edge addEdge(AnalyzedEnergyObject source,
                         AnalyzedEnergyObject destination,
                         List<AnalyzedEnergyObject> affects,
                         long capacity) {
        Edge edge = new Edge(source, destination, affects, capacity);
        Edge reverse = new Edge(destination, source, affects, 0);
        edge.setReverse(reverse);
        reverse.setReverse(edge);
        network.putIfAbsent(source, new ArrayList<>());
        network.putIfAbsent(destination, new ArrayList<>());
        network.get(source).add(edge);
        network.get(destination).add(reverse);
        return edge;
    }

    public List<Edge> getEdges(AnalyzedEnergyObject source) {
        return network.getOrDefault(source, null);
    }

    private void checkAndRebalance(double dt) {
        boolean needsRebalancing = false;

        Consumer<Edge> energyUpdate = (edge) -> {
            edge.setCapacity(Long.MAX_VALUE);
            network.values().stream().flatMap(List::stream).forEach(networkEdge -> networkEdge.setUsed(0));
            calculate();
        };

        for (Edge edge : sourceEdges) {
            long transfer = (long)(edge.getUsed() * dt);

            if (!(edge.getDestination().energyObject() instanceof EnergyNode node)) {
                throw new IllegalArgumentException(edge.getDestination().energyObject().getClass().getName());
            }

            if (node.getCurrentEnergy() < transfer) {
                needsRebalancing = true;
                edge.setCapacity(Math.round(node.getCurrentEnergy() / dt));
                node.onEnergyUpdated(new EnergyListener(
                        true,
                        _ -> energyUpdate.accept(edge),
                        EnergyUpdateType.Add
                ));
            }
        }

        for (Edge edge : sinkEdges) {
            long transfer = (long)(edge.getUsed() * dt);

            if (!(edge.getSource().energyObject() instanceof EnergyNode node)) {
                throw new IllegalArgumentException(edge.getDestination().energyObject().getClass().getName());
            }

            if (node.getEnergyRemaining() < transfer) {
                needsRebalancing = true;
                edge.setCapacity(Math.round(node.getEnergyRemaining() / dt));
                node.onEnergyUpdated(new EnergyListener(
                        true,
                        _ ->  energyUpdate.accept(edge),
                        EnergyUpdateType.Remove
                ));
            }
        }

        if (needsRebalancing) {
            network.values().stream().flatMap(List::stream).forEach(edge -> edge.setUsed(0));
            calculate();
        }
    }

    public void computeNetwork(double dt) {
        checkAndRebalance(dt);

        for (Edge edge : sourceEdges) {
            long transfer = (long)(edge.getUsed() * dt);

            if (!(edge.getDestination().energyObject() instanceof EnergyNode node)) {
                throw new IllegalArgumentException(edge.getDestination().energyObject().getClass().getName());
            }

            node.removeEnergy(transfer);
        }

        for (Edge edge : sinkEdges) {
            long transfer = (long)(edge.getUsed() * dt);

            if (!(edge.getSource().energyObject() instanceof EnergyNode node)) {
                throw new IllegalArgumentException(edge.getDestination().energyObject().getClass().getName());
            }

            node.addEnergy(transfer);
        }
    }

    public void calculate() {
        while (true) {
            List<PathPart> path = getPath(
                    EnergyGroupNetwork.START,
                    EnergyGroupNetwork.END,
                    new ArrayList<>(),
                    new HashSet<>()
            );

            if (path == null) {
                break;
            }

            path = path.reversed();

            long flow = getBottleneckFlow(path);

            for (PathPart pathPart : path) {
                pathPart.edge().addUsed(flow);
                pathPart.edge().getReverse().removeUsed(flow);
            }
        }
    }

    private long getBottleneckFlow(List<PathPart> path) {
        return path
                .stream()
                .map(PathPart::residualEnergy)
                .min(Long::compare)
                .orElse(Long.MAX_VALUE);
    }

    private List<PathPart> getPath(
            AnalyzedEnergyObject start,
            AnalyzedEnergyObject end,
            List<PathPart> path,
            Set<AnalyzedEnergyObject> visited
    ) {
        if (!visited.add(start)) {
            return null;
        }

        if (start == end) {
            return path;
        }

        for (Edge edge : getEdges(start)) {
            long residualCapacity = edge.getCapacity() - edge.getUsed();

            PathPart pathPart = new PathPart(edge, residualCapacity);

            if (residualCapacity > 0) {
                List<PathPart> result = getPath(edge.getDestination(), end, path, visited);

                if (result != null) {
                    path.add(pathPart);
                    return result;
                }
            }
        }

        return null;
    }

    public void print() {
        Set<AnalyzedEnergyObject> visitedNodes = new HashSet<>();

        for (Edge edge : sourceEdges) {
            LOGGER.atInfo().log("Source edge (used: %d / capacity: %d)", edge.getUsed(), edge.getCapacity());
            printEdges(edge.getDestination(), visitedNodes);
        }
    }

    private void printEdges(AnalyzedEnergyObject object, Set<AnalyzedEnergyObject> visitedNodes) {
        if (!visitedNodes.add(object)) {
            return;
        }

        List<Edge> edges = network.get(object);
        LOGGER.atInfo().log("Connections: %d",  edges.size());

        for (Edge edge : edges) {
            if (edge.getCapacity() == 0L) {
                continue;
            }

            LOGGER.atInfo().log("Edge (used: %d / capacity: %d) %s connecting to %s",
                    edge.getUsed(),
                    edge.getCapacity(),
                    edge.getSource().position(),
                    edge.getDestination().position()
            );
            printEdges(edge.getDestination(), visitedNodes);
        }
    }
}
