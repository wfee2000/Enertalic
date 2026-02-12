package com.wfee.enertalic.util;

import java.util.function.Consumer;

public record ReactiveListener<T>(boolean activateOnce, Consumer<T> action, UpdateType updateType) {
    public boolean checkAndApply(T value, UpdateType updateType) {
        if (this.updateType == UpdateType.All || updateType == this.updateType) {
            action.accept(value);

            return activateOnce;
        }

        return false;
    }
}
