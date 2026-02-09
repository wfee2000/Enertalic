package com.wfee.enertalic.util;

import java.util.function.Consumer;

public record EnergyListener(boolean activateOnce, Consumer<Long> action) {
    public void accept(long currentEnergy) {
        action.accept(currentEnergy);
    }
}
