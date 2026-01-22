package com.wfee.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.wfee.Enertalic;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EnergyNode extends EnergyBase {

    @Nonnull
    /* Maybe switch to double later on*/
    public static final KeyedCodec<Long> CURRENT_ENERGY = new KeyedCodec<>("CurrentEnergy", Codec.LONG);
    public static final KeyedCodec<Long> MAX_ENERGY = new KeyedCodec<>("MaxEnergy", Codec.LONG);
    public static final BuilderCodec<EnergyNode> CODEC =
            BuilderCodec
                    .builder(EnergyNode.class, EnergyNode::new, EnergyBase.CODEC)
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

    public void addEnergy(long value)
    {
        this.currentEnergy += value;
        if (this.currentEnergy > this.maxEnergy)
        {
            this.currentEnergy = this.maxEnergy;
            throw new IllegalArgumentException();
        }
    }

    public void removeEnergy(long value)
    {
        this.currentEnergy -= value;
        if (this.currentEnergy < 0)
        {
            this.currentEnergy = 0;
            throw new IllegalArgumentException();
        }
    }

    public long getMaxEnergy()
    {
        return maxEnergy;
    }

    public void setMaxEnergy(long value)
    {
        this.maxEnergy = value;
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
}
