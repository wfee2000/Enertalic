package com.wfee.enertalic.util;

import java.util.function.Consumer;

public record EnergyListener(boolean activateOnce, Consumer<Long> action, EnergyUpdateType energyUpdateType) {
    public void accept(long currentEnergy) {
        action.accept(currentEnergy);
    }
}
