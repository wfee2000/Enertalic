package com.wfee.enertalic.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class BlockSides<T> {
    protected final Map<Direction, T> sides;

    public BlockSides(T all) {
        this(all, all, all, all, all, all);
    }

    public BlockSides(T east, T west, T up, T down, T south, T north) {
        sides = new HashMap<>() {{
            put(Direction.East, east);
            put(Direction.West, west);
            put(Direction.Up, up);
            put(Direction.Down, down);
            put(Direction.South, south);
            put(Direction.North, north);
        }};
    }

    public BlockSides(Map<Direction, T> sides) {
        this.sides = new HashMap<>();
        this.sides.putAll(sides);
    }

    public T getDirection(Direction direction) {
        return sides.get(direction);
    }

    public List<Map.Entry<Direction,T>> getEntries(Predicate<Map.Entry<Direction, T>> predicate) {
        return sides.entrySet().stream().filter(predicate).toList();
    }

    public void setDirection(Direction direction, T config) {
        sides.put(direction, config);
    }

    public long countMatching(Predicate<T> predicate) {
        return sides.values().stream().filter(predicate).count();
    }
}
