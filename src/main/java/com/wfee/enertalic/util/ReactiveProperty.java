package com.wfee.enertalic.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class ReactiveProperty<T> {
    private T value;
    private final List<ReactiveListener<T>> listeners = new ArrayList<>();

    public ReactiveProperty(T value) {
        this.value = value;
    }

    public T get() {
        return value;
    }

    public void modify(Function<T,T> modificator) {
        modify(modificator, UpdateType.All);
    }

    public void modify(Function<T,T> modificator, UpdateType updateType) {
        set(modificator.apply(value), updateType);
    }

    public void set(T value, UpdateType updateType) {
        if (Objects.equals(this.value, value)) {
            return;
        }

        this.value = value;

        listeners.forEach(listener -> {
            if (listener.checkAndApply(value, updateType)) {
                listeners.remove(listener);
            }
        });
    }

    public void set(T value) {
        set(value, UpdateType.All);
    }

    public void onChange(ReactiveListener<T> listener) {
        listeners.add(listener);
    }

    public void observe(ReactiveListener<T> listener) {
        onChange(listener);
        listener.action().accept(get());
    }

    public void removeListener(ReactiveListener<T> listener) {
        listeners.remove(listener);
    }

    public <R> R computeResult(Function<T, R> computer) {
        return computer.apply(get());
    }
}
