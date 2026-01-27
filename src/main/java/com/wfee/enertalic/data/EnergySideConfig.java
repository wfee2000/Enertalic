package com.wfee.enertalic.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.logger.HytaleLogger;
import com.wfee.enertalic.util.Direction;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class EnergySideConfig {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public final static KeyedCodec<Integer>[] DIRECTIONS = new KeyedCodec[]{
            new KeyedCodec<>("East", Codec.INTEGER),
            new KeyedCodec<>("West", Codec.INTEGER),
            new KeyedCodec<>("Up", Codec.INTEGER),
            new KeyedCodec<>("Down", Codec.INTEGER),
            new KeyedCodec<>("South", Codec.INTEGER),
            new KeyedCodec<>("North", Codec.INTEGER),
    };
    public final static BuilderCodec<EnergySideConfig> CODEC;

    static {
        BuilderCodec.Builder<EnergySideConfig> builder = BuilderCodec.builder(EnergySideConfig.class, EnergySideConfig::new);

        for (int i = 0; i < 6; i++) {
            Direction direction = Direction.values()[i];
            builder = builder
                    .append(DIRECTIONS[i],
                            (object, value) ->
                                    object.setDirection(direction, EnergyConfig.values()[value]),
                            object -> object.getDirection(direction).ordinal())
                    .add();
        }

        CODEC = builder.build();
    }

    private final Map<Direction, EnergyConfig> sides;

    public EnergySideConfig() {
        this(EnergyConfig.OFF, EnergyConfig.OFF, EnergyConfig.OFF, EnergyConfig.OFF, EnergyConfig.OFF, EnergyConfig.OFF);
    }

    public EnergySideConfig(EnergyConfig east, EnergyConfig west, EnergyConfig up, EnergyConfig down, EnergyConfig south, EnergyConfig north) {
        sides = new HashMap<>() {{
            put(Direction.East, east);
            put(Direction.West, west);
            put(Direction.Up, up);
            put(Direction.Down, down);
            put(Direction.South, south);
            put(Direction.North, north);
        }};
    }

    public EnergyConfig getDirection(Direction direction) {
        return sides.get(direction);
    }

    public void setDirection(Direction direction, EnergyConfig config) {
        sides.put(direction, config);
    }

    public long countMatching(Predicate<EnergyConfig> predicate) {
        return sides.values().stream().filter(predicate).count();
    }

    @Override
    public String toString() {
        return sides.values().stream().map(side -> String.valueOf(side.ordinal())).collect(Collectors.joining());
    }
}
