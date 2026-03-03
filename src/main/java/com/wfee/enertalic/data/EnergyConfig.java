package com.wfee.enertalic.data;

public enum EnergyConfig {
    IN,
    OUT,
    INOUT,
    OFF;

    public boolean canExport() {
        return this == INOUT || this == OUT;
    }

    public boolean canImport() {
        return this == IN || this == INOUT;
    }

    public boolean connectsTo(EnergyConfig target) {
        if (this == OFF || target == OFF) {
            return false;
        }

        if (this == INOUT || target == INOUT) {
            return true;
        }

        return this != target;
    }
}
