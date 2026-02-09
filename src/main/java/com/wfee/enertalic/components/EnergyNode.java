package com.wfee.enertalic.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.wfee.enertalic.Enertalic;
import com.wfee.enertalic.util.EnergyListener;
import com.wfee.enertalic.util.EnergyUpdateType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

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
    private final List<EnergyListener> energyUpdateListeners = new ArrayList<>();

    public long getCurrentEnergy()
    {
        return currentEnergy;
    }

    public void addEnergy(long value) {
        if (getEnergyRemaining() < value)
        {
            this.currentEnergy = this.maxEnergy;
            throw new IllegalArgumentException();
        }

        this.currentEnergy += value;
        acceptListeners(EnergyUpdateType.Add);
    }

    public void removeEnergy(long value) {
        if (currentEnergy < value) {
            throw new IllegalArgumentException(String.format("Can not remove %d energy from %d stored", value, this.currentEnergy));
        }

        this.currentEnergy -= value;
        acceptListeners(EnergyUpdateType.Remove);
    }

    public long getMaxEnergy() {
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

    public void onEnergyUpdated(EnergyListener listener) {
        this.energyUpdateListeners.add(listener);
    }

    private void acceptListeners(EnergyUpdateType updateType) {
        this.energyUpdateListeners.forEach(listener -> {
            if (listener.energyUpdateType() == EnergyUpdateType.All || listener.energyUpdateType() == updateType) {
                listener.accept(this.currentEnergy);

                if (listener.activateOnce()) {
                    this.energyUpdateListeners.remove(listener);
                }
            }
        });
    }
}
