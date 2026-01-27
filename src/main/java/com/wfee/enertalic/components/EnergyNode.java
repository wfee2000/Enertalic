package com.wfee.enertalic.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.wfee.enertalic.Enertalic;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EnergyNode extends EnergyObject {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Nonnull
    public static final KeyedCodec<Long> CURRENT_ENERGY = new KeyedCodec<>("CurrentEnergy", Codec.LONG);
    public static final KeyedCodec<Long> MAX_ENERGY = new KeyedCodec<>("MaxEnergy", Codec.LONG);
    public static final BuilderCodec<EnergyNode> CODEC =
            BuilderCodec
                    .builder(EnergyNode.class, EnergyNode::new, EnergyObject.CODEC)
                        .append(CURRENT_ENERGY,
                                (object, value) -> object.currentEnergy = value,
                                object -> object.currentEnergy)
                        .add()
                        .append(MAX_ENERGY,
                                (object, value) -> object.maxEnergy = value,
                                object -> object.maxEnergy)
                        .add()
                    .build();

    private long currentEnergy = 0L;
    private long maxEnergy = 0L;

    public long getCurrentEnergy()
    {
        return currentEnergy;
    }

    public void setCurrentEnergy(long value)
    {
        this.currentEnergy = value;
    }

    public void addEnergy(long value) {
        if (getEnergyRemaining() < value)
        {
            this.currentEnergy = this.maxEnergy;
            throw new IllegalArgumentException();
        }

        this.currentEnergy += value;
    }

    public void removeEnergy(long value) {
        if (currentEnergy < value) {
            throw new IllegalArgumentException(String.format("Can not remove %d energy from %d stored", value, this.currentEnergy));
        }

        this.currentEnergy -= value;
    }

    public long getMaxEnergy()
    {
        return maxEnergy;
    }

    @Nonnull
    public static ComponentType<ChunkStore, EnergyNode> getComponentType() {
        return Enertalic.get().getEnergyNodeComponentType();
    }

    @Nullable
    @Override
    public Component<ChunkStore> clone() {
        EnergyNode clone = (EnergyNode) super.clone();

        if (clone == null) return null;

        clone.currentEnergy = this.currentEnergy;
        clone.maxEnergy = this.maxEnergy;
        return clone;
    }

    public long getEnergyRemaining() {
        return maxEnergy - currentEnergy;
    }
}
