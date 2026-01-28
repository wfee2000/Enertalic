package com.wfee.enertalic.data.network;

import com.wfee.enertalic.util.AnalyzedEnergyObject;

import java.util.List;

public final class Edge {
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

    public Edge getReverse() {
        return reverse;
    }

    public void setReverse(Edge reverse) {
        this.reverse = reverse;
    }

    public AnalyzedEnergyObject getDestination() {
        return destination;
    }

    public long getCapacity() {
        return capacity;
    }

    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }

    public AnalyzedEnergyObject getSource() {
        return source;
    }

    public long getUsed() {
        return used;
    }

    public void setUsed(long used) {
        this.used = used;
    }

    public void addUsed(long flow) {
        this.used += flow;
    }

    public void removeUsed(long flow) {
        this.used -= flow;
    }
}
