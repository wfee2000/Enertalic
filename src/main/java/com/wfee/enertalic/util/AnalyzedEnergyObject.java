package com.wfee.enertalic.util;

import com.hypixel.hytale.math.vector.Vector3i;
import com.wfee.enertalic.components.EnergyObject;

import java.util.Objects;

public class AnalyzedEnergyObject {
    private final EnergyObject energyObject;
    private final Vector3i position;

    public AnalyzedEnergyObject(EnergyObject energyObject, Vector3i position) {
        this.energyObject = energyObject;
        this.position = position;
    }

    public EnergyObject getEnergyObject() {
        return energyObject;
    }

    public Vector3i getPosition() {
        return position;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AnalyzedEnergyObject that)) return false;

        return position == that.position ||
                position != null && that.position != null && position.equals(that.position) && energyObject == that.energyObject;
    }

    @Override
    public int hashCode() {
        return Objects.hash(energyObject, position);
    }
}
