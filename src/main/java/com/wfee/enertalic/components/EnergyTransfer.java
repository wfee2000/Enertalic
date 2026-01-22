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

public class EnergyTransfer extends EnergyBase {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final KeyedCodec<Long> MAX_TRANSFER_RATE = new KeyedCodec<>("MaxTransferRate", Codec.LONG);
    public static final BuilderCodec<EnergyTransfer> CODEC =
            BuilderCodec
                    .builder(EnergyTransfer.class, EnergyTransfer::new, EnergyBase.CODEC)
                        .append(MAX_TRANSFER_RATE,
                                (object, value) -> object.maxTransferRate = value,
                                object -> object.maxTransferRate)
                        .add()
                    .build();

    private long maxTransferRate;
    private long currentTransferRate;

    public long getMaxTransferRate() {
        return maxTransferRate;
    }

    public void setMaxTransferRate(long maxTransferRate) {
        this.maxTransferRate = maxTransferRate;
    }

    public long getCurrentTransferRate() {
        return currentTransferRate;
    }

    public void setCurrentTransferRate(long currentTransferRate) {
        this.currentTransferRate = currentTransferRate;
    }

    @Nonnull
    public static ComponentType<ChunkStore, EnergyTransfer> getComponentType() {
        return Enertalic.get().getEnergyTransferComponentType();
    }

    @Nullable
    @Override
    public Component<ChunkStore> clone() {
        EnergyTransfer clone = (EnergyTransfer) super.clone();

        if (clone == null) return null;

        clone.setCurrentTransferRate(currentTransferRate);
        clone.setMaxTransferRate(maxTransferRate);

        return clone;
    }
}
