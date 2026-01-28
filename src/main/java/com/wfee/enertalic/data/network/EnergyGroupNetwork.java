package com.wfee.enertalic.data.network;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.wfee.enertalic.components.EnergyNode;
import com.wfee.enertalic.components.EnergyTransfer;
import com.wfee.enertalic.util.AnalyzedEnergyObject;
import com.wfee.enertalic.util.Direction;
import com.wfee.enertalic.util.EnergyGroup;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

public class EnergyGroupNetwork {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClassFull();

    private final Map<AnalyzedEnergyObject, List<Edge>> network;
    private final Set<Edge> sourceEdges;
    private final Set<Edge> sinkEdges;

    private static final AnalyzedEnergyObject START = new AnalyzedEnergyObject(new EnergyNode(), null);
    private static final AnalyzedEnergyObject END = new AnalyzedEnergyObject(new EnergyNode(), null);

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
            Set<Vector3i> visitedPositions = new HashSet<>();

            traverseGroupFromNode(source, group, visitedPositions, null);
        }

        for (AnalyzedEnergyObject destination : group.getConsumers())
        {
            sinkEdges.add(addSuperEdge(destination, END));
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

            getMatchingObject.accept(group.getConsumers());
            getMatchingObject.accept(group.getTransfers());
        }

        return surroundingObjects;
    }

    private Edge addSuperEdge(AnalyzedEnergyObject source, AnalyzedEnergyObject destination) {
        return addSuperEdge(source, destination, new ArrayList<>(), Long.MAX_VALUE);
    }

    private Edge addSuperEdge(AnalyzedEnergyObject source,
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

        for (Edge edge : sourceEdges) {
            long transfer = getTransferRate(dt, edge);

            if (!(edge.getDestination().getEnergyObject() instanceof EnergyNode node)) {
                throw new IllegalArgumentException(edge.getDestination().getEnergyObject().getClass().getName());
            }

            if (node.getCurrentEnergy() < transfer) {
                needsRebalancing = true;
                edge.setCapacity(Math.round(node.getCurrentEnergy() / dt));
            }
        }

        for (Edge edge : sinkEdges) {
            long transfer = getTransferRate(dt, edge);

            if (!(edge.getDestination().getEnergyObject() instanceof EnergyNode node)) {
                throw new IllegalArgumentException(edge.getDestination().getEnergyObject().getClass().getName());
            }

            if (node.getEnergyRemaining() < transfer) {
                needsRebalancing = true;
                edge.setCapacity(Math.round(node.getEnergyRemaining() / dt));
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
            long transfer = getTransferRate(dt, edge);

            if (!(edge.getDestination().getEnergyObject() instanceof EnergyNode node)) {
                throw new IllegalArgumentException(edge.getDestination().getEnergyObject().getClass().getName());
            }

            node.removeEnergy(transfer);
        }

        for (Edge edge : sinkEdges) {
            long transfer = getTransferRate(dt, edge);

            if (!(edge.getDestination().getEnergyObject() instanceof EnergyNode node)) {
                throw new IllegalArgumentException(edge.getDestination().getEnergyObject().getClass().getName());
            }

            node.addEnergy(transfer);
            LOGGER.atInfo().log("Transferred %d Energy", transfer);
        }
    }

    private long getTransferRate(double dt, Edge edge) {
        return (long)(network
                .get(edge.getDestination())
                .stream()
                .filter(extremityEdge -> extremityEdge.getDestination() != edge.getSource())
                .mapToLong(Edge::getUsed)
                .sum() * dt);
    }

    public void calculate() {
        List<PathPart> path = getPath(EnergyGroupNetwork.START, EnergyGroupNetwork.END, new ArrayList<>());

        while (path != null) {
            long flow = getBottleneckFlow(path);

            for (PathPart pathPart : path) {
                pathPart.edge().addUsed(flow);
                pathPart.edge().getReverse().removeUsed(flow);
                path = getPath(EnergyGroupNetwork.START, EnergyGroupNetwork.END, new ArrayList<>());
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

    private List<PathPart> getPath(AnalyzedEnergyObject start, AnalyzedEnergyObject end, List<PathPart> path) {
        if (start == end) {
            return path;
        }

        for (Edge edge : getEdges(start)) {
            long residualCapacity = edge.getCapacity() - edge.getUsed();

            PathPart pathPart = new PathPart(edge, residualCapacity);

            if (residualCapacity > 0 && !path.contains(pathPart)) {
                path.add(pathPart);

                List<PathPart> result = getPath(edge.getDestination(), end, path);

                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }
}
