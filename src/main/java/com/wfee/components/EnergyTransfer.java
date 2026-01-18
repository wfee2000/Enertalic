package com.wfee.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.wfee.EnergyModule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EnergyTransfer implements Component<EntityStore> {
    public static final KeyedCodec<Long> TRANSFER_RATE = new KeyedCodec<Long>("TransferRate", Codec.LONG);
    public static final BuilderCodec<EnergyTransfer> CODEC =
            BuilderCodec
                    .builder(EnergyTransfer.class, EnergyTransfer::new)
                    .append(TRANSFER_RATE,
                            (object, value) -> object.transferRate = value,
                            object -> object.transferRate)
                    .add()
                    .build();

    private Long transferRate = 0L;

    @Nonnull
    public static ComponentType<EntityStore, EnergyTransfer> getComponentType() {
        return EnergyModule.get().getEnergyTransferComponentType();
    }

    @Nullable
    @Override
    public Component<EntityStore> clone() {
        return new EnergyTransfer();
    }
}
