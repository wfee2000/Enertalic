package com.wfee.enertalic.util;

import com.hypixel.hytale.math.vector.Vector3i;
import com.wfee.enertalic.components.EnergyObject;

import java.util.Objects;

public record AnalyzedEnergyObject(EnergyObject energyObject, Vector3i position, boolean isExtension) {

    @Override
    public boolean equals(Object o) {
        return o instanceof AnalyzedEnergyObject(EnergyObject object, Vector3i position1, boolean isExtension1) &&
                Objects.equals(position, position1) &&
                energyObject == object &&
                isExtension == isExtension1;
    }
}
