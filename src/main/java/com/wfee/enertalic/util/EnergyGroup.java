package com.wfee.enertalic.util;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.wfee.enertalic.components.EnergyObject;
import com.wfee.enertalic.data.network.EnergyGroupNetwork;

import java.util.*;
import java.util.function.Predicate;

public final class EnergyGroup {
    private final static HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final Set<AnalyzedEnergyObject> consumers;
    private final Set<AnalyzedEnergyObject> providers;
    private final Set<AnalyzedEnergyObject> transfers;
    private final Map<AnalyzedEnergyObject, List<Pair<Direction, AnalyzedEnergyObject>>> surroundingBlocks;
    private EnergyGroupNetwork network;

    public EnergyGroup(
            Set<AnalyzedEnergyObject> consumers,
            Set<AnalyzedEnergyObject> providers,
            Set<AnalyzedEnergyObject> transfers,
            HashMap<AnalyzedEnergyObject, List<Pair<Direction, AnalyzedEnergyObject>>> surroundingBlocks
    ) {
        this.consumers = consumers;
        this.providers = providers;
        this.transfers = transfers;
        this.surroundingBlocks = surroundingBlocks;
    }

    public boolean contains(Vector3i position) {
        return getAllObjects()
                .stream()
                .map(AnalyzedEnergyObject::position)
                .anyMatch(analyzedObject -> analyzedObject.equals(position));
    }

    public AnalyzedEnergyObject getObjectForPosition(Vector3i position) {
        return surroundingBlocks
                .keySet()
                .stream()
                .filter(object -> object.position().equals(position))
                .findAny()
                .orElse(null);
    }

    public boolean contains(EnergyObject object) {
        return getAllObjects()
                .stream()
                .map(AnalyzedEnergyObject::energyObject)
                .anyMatch(analyzedObject -> analyzedObject == object);
    }

    public Map<AnalyzedEnergyObject, List<Pair<Direction, AnalyzedEnergyObject>>> getSurroundingBlocks() {
        return surroundingBlocks;
    }

    public List<AnalyzedEnergyObject> getAllObjects() {
        List<AnalyzedEnergyObject> union = new ArrayList<>();
        union.addAll(transfers);
        union.addAll(providers);
        union.addAll(consumers);
        return union;
    }

    public Set<AnalyzedEnergyObject> getConsumers() {
        return consumers;
    }

    public Set<AnalyzedEnergyObject> getProviders() {
        return providers;
    }

    public Set<AnalyzedEnergyObject> getTransfers() {
        return transfers;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (EnergyGroup) obj;
        return Objects.equals(this.consumers, that.consumers) &&
                Objects.equals(this.providers, that.providers) &&
                Objects.equals(this.transfers, that.transfers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(consumers, providers, transfers);
    }

    public EnergyGroupNetwork getNetwork() {
        return network;
    }

    public void setNetwork(EnergyGroupNetwork network) {
        this.network = network;
    }

    private boolean removeMatching(Predicate<AnalyzedEnergyObject> predicate) {
        return this.consumers.removeIf(predicate) ||
                this.providers.removeIf(predicate) ||
                this.transfers.removeIf(predicate);
    }

    public boolean remove(AnalyzedEnergyObject object) {
        boolean wasRemoved = removeMatching(object1 -> object1.equals(object));

        surroundingBlocks.remove(object);
        surroundingBlocks
                .values()
                .forEach(blocks ->
                        blocks.removeIf(pair -> pair.item2().equals(object))
                );

        return wasRemoved;
    }

    public boolean isEmpty() {
        return this.consumers.isEmpty() && this.providers.isEmpty() && this.transfers.isEmpty();
    }

    public List<EnergyGroup> checkConnections() {
        if (isEmpty()) {
            return null;
        }

        HashSet<AnalyzedEnergyObject> visited = new HashSet<>();
        List<AnalyzedEnergyObject> objects = getAllObjects();

        fillConnections(objects.getFirst(), visited);

        if (visited.size() == objects.size()) {
            return null;
        }

        List<EnergyGroup> groups = new ArrayList<>();

        while (true) {
            Set<AnalyzedEnergyObject> providers = new HashSet<>();
            Set<AnalyzedEnergyObject> transfers = new HashSet<>();
            Set<AnalyzedEnergyObject> consumers = new HashSet<>();
            HashMap<AnalyzedEnergyObject, List<Pair<Direction, AnalyzedEnergyObject>>> surroundingBlocks = new HashMap<>();

            for (AnalyzedEnergyObject object : visited) {
                if (this.providers.contains(object)) {
                    providers.add(object);
                }

                if (this.transfers.contains(object)) {
                    transfers.add(object);
                }

                if (this.consumers.contains(object)) {
                    consumers.add(object);
                }

                objects.remove(object);
                surroundingBlocks.put(object, this.surroundingBlocks.get(object));
            }

            groups.add(new EnergyGroup(consumers, providers, transfers, surroundingBlocks));
            visited.clear();

            if (objects.isEmpty()) {
                break;
            }

            fillConnections(objects.getFirst(), visited);
        }

        return groups;
    }

    private void fillConnections(AnalyzedEnergyObject start, Set<AnalyzedEnergyObject> visited) {
        if (!visited.add(start)) {
            return;
        }

        surroundingBlocks.get(start)
                .forEach(object -> fillConnections(object.item2(), visited));
    }
}
