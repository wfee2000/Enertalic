package com.wfee.enertalic.util;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;

public enum Direction {
    East(Vector3i.EAST),
    West(Vector3i.WEST),
    Up(Vector3i.UP),
    Down(Vector3i.DOWN),
    South(Vector3i.SOUTH),
    North(Vector3i.NORTH);

    private final Vector3i offset;

    Direction(Vector3i offset) {
        this.offset = offset;
    }

    public Vector3i getOffset() {
        return offset;
    }

    public Direction getOpposite() {
        return switch (this) {
            case East -> West;
            case West -> East;
            case Up -> Down;
            case Down -> Up;
            case South -> North;
            case North -> South;
        };
    }

    public RotationTuple getRotation() {
        return switch (this) {
            case North -> RotationTuple.NONE;
            case West -> RotationTuple.of(Rotation.Ninety,  Rotation.None);
            case South -> RotationTuple.of(Rotation.OneEighty, Rotation.None);
            case East -> RotationTuple.of(Rotation.TwoSeventy, Rotation.None);
            case Up -> RotationTuple.of(Rotation.None, Rotation.Ninety);
            case Down -> RotationTuple.of(Rotation.None, Rotation.TwoSeventy);
        };
    }

    public Direction getLeft() {
        return switch (this) {
            case North, Up -> West;
            case West -> South;
            case South, Down -> East;
            case East -> North;
        };
    }
}
