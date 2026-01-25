package com.wfee.enertalic.util;

import com.hypixel.hytale.math.vector.Vector3i;

public enum Direction {
    East(new Vector3i(1,0,0)),
    West(new Vector3i(-1,0,0)),
    Up(new Vector3i(0,1,0)),
    Down(new Vector3i(0,-1,0)),
    South(new Vector3i(0,0,1)),
    North(new Vector3i(0,0,-1));

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
}
