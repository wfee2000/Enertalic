package com.wfee.enertalic.components;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.wfee.enertalic.data.EnergySideConfig;

import javax.annotation.Nullable;

public abstract class EnergyBase implements Component<ChunkStore> {
    public static final KeyedCodec<EnergySideConfig> ENERGY_SIDE_CONFIG = new KeyedCodec<>("EnergySideConfig", EnergySideConfig.CODEC);
    protected static final BuilderCodec<EnergyBase> CODEC =
            BuilderCodec.abstractBuilder(EnergyBase.class)
                    .append(ENERGY_SIDE_CONFIG,
                            EnergyBase::setEnergySideConfig,
                            EnergyBase::getEnergySideConfig)
                    .add()
                    .build();

    @Nullable
    @Override
    public Component<ChunkStore> clone() {
        try {
            EnergyBase clone = (EnergyBase) super.clone();
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
        this.energySideConfig = value;
    }
}
