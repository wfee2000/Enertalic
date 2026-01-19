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

public class EnergyNode implements Component<ChunkStore> {

    @Nonnull
    /* Maybe switch to double later on*/
    public static final KeyedCodec<Long> CURRENT_ENERGY = new KeyedCodec<>("CurrentEnergy", Codec.LONG);
    public static final KeyedCodec<Long> MAX_ENERGY = new KeyedCodec<>("MaxEnergy", Codec.LONG);
    public static final BuilderCodec<EnergyNode> CODEC =
            BuilderCodec
                    .builder(EnergyNode.class, EnergyNode::new)
                        .append(CURRENT_ENERGY,
                                (object, value) -> object.currentEnergy = value,
                                object -> object.currentEnergy)
                        .add()
                        .append(MAX_ENERGY,
                                (object, value) -> object.maxEnergy = value,
                                object -> object.maxEnergy)
                        .add()
                    .build();

    private Long currentEnergy = 0L;
    private Long maxEnergy = 0L;

    @Nonnull
    public static ComponentType<ChunkStore, EnergyNode> getComponentType() {
        return Enertalic.get().getEnergyNodeComponentType();
    }

    @Nullable
    @Override
    public Component<ChunkStore> clone() {
        return new EnergyNode();
    }
}
