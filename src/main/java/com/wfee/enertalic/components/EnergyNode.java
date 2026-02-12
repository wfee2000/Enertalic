package com.wfee.enertalic.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.wfee.enertalic.Enertalic;
import com.wfee.enertalic.util.ReactiveNumericProperty;

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
                                (object, value) -> object.currentEnergy.set(value),
                                object -> object.currentEnergy.get())
                        .add()
                        .append(MAX_ENERGY,
                                (object, value) -> object.maxEnergy = value,
                                object -> object.maxEnergy)
                        .add()
                    .build();

    private final ReactiveNumericProperty currentEnergy = new ReactiveNumericProperty(0L);
    private long maxEnergy = 0L;

    public EnergyNode() {
        super();
    }

    public EnergyNode(EnergyNode energyNode) {
        super(energyNode);
        this.currentEnergy.set(energyNode.currentEnergy.get());
        this.maxEnergy = energyNode.maxEnergy;
    }

    public ReactiveNumericProperty getCurrentEnergy()
    {
        return currentEnergy;
    }

    public long getMaxEnergy() {
        return maxEnergy;
    }

    @Nonnull
    public static ComponentType<ChunkStore, EnergyNode> getComponentType() {
        return Enertalic.get().getEnergyNodeComponentType();
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Nullable
    @Override
    public Component<ChunkStore> clone() {
        return new EnergyNode(this);
    }

    public long getEnergyRemaining() {
        return currentEnergy.computeResult(currentEnergy -> maxEnergy - currentEnergy);
    }
}
