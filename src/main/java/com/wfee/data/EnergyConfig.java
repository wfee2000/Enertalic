package com.wfee.data;

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
}
