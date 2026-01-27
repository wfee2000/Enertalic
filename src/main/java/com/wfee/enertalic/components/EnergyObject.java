package com.wfee.enertalic.components;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.wfee.enertalic.data.EnergyConfig;
import com.wfee.enertalic.data.EnergySideConfig;

import javax.annotation.Nullable;

public abstract class EnergyObject implements Component<ChunkStore> {
    public static final KeyedCodec<EnergySideConfig> ENERGY_SIDE_CONFIG = new KeyedCodec<>("EnergySideConfig", EnergySideConfig.CODEC);
    protected static final BuilderCodec<EnergyObject> CODEC =
            BuilderCodec.abstractBuilder(EnergyObject.class)
                    .append(ENERGY_SIDE_CONFIG,
                            EnergyObject::setEnergySideConfig,
                            EnergyObject::getEnergySideConfig)
                    .add()
                    .build();

    @Nullable
    @Override
    public Component<ChunkStore> clone() {
        try {
            EnergyObject clone = (EnergyObject) super.clone();
            clone.setEnergySideConfig(this.energySideConfig);
            return clone;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    private EnergySideConfig energySideConfig;

    public EnergySideConfig getEnergySideConfig()
    {
        return energySideConfig;
    }

    public void setEnergySideConfig(EnergySideConfig value)
    {
        if (this instanceof EnergyNode && value.countMatching(config -> config == EnergyConfig.INOUT) != 0) {
            throw new IllegalArgumentException("In and Outputting simultaneously is currently not supported");
        }

        this.energySideConfig = value;
    }
}
