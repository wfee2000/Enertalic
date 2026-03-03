package com.wfee.enertalic.components;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.wfee.enertalic.data.EnergySideConfig;
import com.wfee.enertalic.util.ReactiveProperty;

import javax.annotation.Nullable;

public abstract class EnergyObject implements Component<ChunkStore> {
    public static final KeyedCodec<EnergySideConfig> ENERGY_SIDE_CONFIG = new KeyedCodec<>("EnergySideConfig", EnergySideConfig.CODEC);
    protected static final BuilderCodec<EnergyObject> CODEC =
            BuilderCodec.abstractBuilder(EnergyObject.class)
                    .append(ENERGY_SIDE_CONFIG,
                            (energyObject, value) -> energyObject.energySideConfig.set(value),
                            energyObject -> energyObject.getEnergySideConfig().get())
                    .add()
                    .build();

    public EnergyObject() {}

    public EnergyObject(EnergyObject energyObject) {
        this.energySideConfig.set(energyObject.getEnergySideConfig().get());
    }

    @Nullable
    @Override
    public Component<ChunkStore> clone() {
        try {
            EnergyObject clone = (EnergyObject) super.clone();
            clone.energySideConfig.set(this.energySideConfig.get());
            return clone;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    private final ReactiveProperty<EnergySideConfig> energySideConfig = new ReactiveProperty<>(new EnergySideConfig());

    public ReactiveProperty<EnergySideConfig> getEnergySideConfig() {
        return energySideConfig;
    }

    @Nullable
    public static EnergyObject fromWorld(World world, Vector3i position) {
        Holder<ChunkStore> holder = world.getBlockComponentHolder(position.x, position.y, position.z);

        if (holder == null) {
            return null;
        }

        EnergyObject object = holder.getComponent(EnergyNode.getComponentType());

        if (object == null) {
            object = holder.getComponent(EnergyTransfer.getComponentType());
        }

        return object;
    }
}
