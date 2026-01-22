package com.wfee.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.npc.util.NPCPhysicsMath;

import java.util.Arrays;
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
            NPCPhysicsMath.Direction direction = NPCPhysicsMath.Direction.values()[i];
            builder = builder
                    .append(DIRECTIONS[i],
                            (object, value) ->
                                    object.setDirection(direction, EnergyConfig.values()[value]),
                            object -> object.getDirection(direction).ordinal())
                    .add();
        }

        CODEC = builder.build();
    }

    private final EnergyConfig[] sides;

    public EnergySideConfig() {
        this(EnergyConfig.OFF, EnergyConfig.OFF, EnergyConfig.OFF, EnergyConfig.OFF, EnergyConfig.OFF, EnergyConfig.OFF);
    }

    public EnergySideConfig(EnergyConfig east, EnergyConfig west, EnergyConfig up, EnergyConfig down, EnergyConfig south, EnergyConfig north) {
        sides = new EnergyConfig[]{east,west,up,down,south,north};
    }

    public EnergyConfig getEast() {
        return sides[0];
    }

    public void setEast(EnergyConfig east) {
        sides[0] = east;
    }

    public EnergyConfig getWest() {
        return sides[1];
    }

    public void setWest(EnergyConfig west) {
        sides[1] = west;
    }

    public EnergyConfig getUp() {
        return sides[2];
    }

    public void setUp(EnergyConfig up) {
        sides[2] = up;
    }

    public EnergyConfig getDown() {
        return sides[3];
    }

    public void setDown(EnergyConfig down) {
        sides[3] = down;
    }

    public EnergyConfig getSouth() {
        return sides[5];
    }

    public void setSouth(EnergyConfig south) {
        sides[5] = south;
    }

    public EnergyConfig getNorth() {
        return sides[5];
    }

    public void setNorth(EnergyConfig north) {
        sides[5] = north;
    }

    public EnergyConfig getDirection(NPCPhysicsMath.Direction direction) {
        return sides[direction.ordinal()];
    }

    public void setDirection(NPCPhysicsMath.Direction direction, EnergyConfig config) {
        sides[direction.ordinal()] = config;
    }

    @Override
    public String toString() {
        return Arrays.stream(sides).map(side -> String.valueOf(side.ordinal())).collect(Collectors.joining());
    }
}
