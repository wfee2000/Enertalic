package com.wfee.enertalic.util;

import java.util.Set;

public record EnergyGroup(
        Set<AnalyzedEnergyObject> consumers,
        Set<AnalyzedEnergyObject> providers,
        Set<AnalyzedEnergyObject> transfers
) {}
