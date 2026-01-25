package com.wfee.enertalic.util;

import com.wfee.enertalic.components.EnergyNode;
import com.wfee.enertalic.components.EnergyTransfer;

import java.util.List;

public record EnergyTraversal(EnergyNode destination, List<EnergyTransfer> path) {}
