package com.wfee.enertalic.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.logger.HytaleLogger;
import com.wfee.enertalic.util.BlockSides;
import com.wfee.enertalic.util.Direction;

public class EnergySideConfig extends BlockSides<EnergyConfig> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public final static BuilderCodec<EnergySideConfig> CODEC;

    static {
        BuilderCodec.Builder<EnergySideConfig> builder = BuilderCodec.builder(EnergySideConfig.class, EnergySideConfig::new);

        for (Direction direction : Direction.values()) {
            builder = builder
                    .append(new KeyedCodec<>(direction.name(), Codec.INTEGER),
                            (object, value) ->
                                    object.setDirection(direction, EnergyConfig.values()[value]),
                            object -> object.getDirection(direction).ordinal())
                    .add();
        }

        CODEC = builder.build();
    }

    public EnergySideConfig() {
        super(EnergyConfig.OFF);
    }

    public EnergySideConfig(EnergySideConfig energySideConfig) {
        super(energySideConfig.sides);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof EnergySideConfig config)) return false;

        for (Direction direction : Direction.values()) {
            if (!sides.get(direction).equals(config.sides.get(direction))) {
                return false;
            }
        }

        return true;
    }
}
