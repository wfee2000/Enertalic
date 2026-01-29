package com.wfee.enertalic.util;

import com.hypixel.hytale.math.vector.Vector3i;
import com.wfee.enertalic.components.EnergyObject;
import com.wfee.enertalic.data.network.EnergyGroupNetwork;

import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class EnergyGroup {
    private final Set<AnalyzedEnergyObject> consumers;
    private final Set<AnalyzedEnergyObject> providers;
    private final Set<AnalyzedEnergyObject> transfers;
    private EnergyGroupNetwork network;

    public EnergyGroup(
            Set<AnalyzedEnergyObject> consumers,
            Set<AnalyzedEnergyObject> providers,
            Set<AnalyzedEnergyObject> transfers
    ) {
        this.consumers = consumers;
        this.providers = providers;
        this.transfers = transfers;
    }

    public boolean contains(Vector3i position) {
        return getAllObjects().anyMatch(analyzedObject -> analyzedObject.position().equals(position));
    }

    public boolean contains(EnergyObject object) {
        return getAllObjects().anyMatch(consumer -> consumer.energyObject() == object);
    }

    private Stream<AnalyzedEnergyObject> getAllObjects() {
        return Stream.concat(Stream.concat(consumers.stream(), providers.stream()),  transfers.stream());
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

    public boolean remove(EnergyObject object) {
        Predicate<AnalyzedEnergyObject> equalsObject =
                analyzedObject -> analyzedObject.energyObject() == object;
        return this.consumers.removeIf(equalsObject) ||
                this.providers.removeIf(equalsObject) ||
                this.transfers.removeIf(equalsObject);
    }
}
