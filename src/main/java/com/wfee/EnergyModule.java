package com.wfee;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.wfee.components.EnergyNode;
import com.wfee.components.EnergyTransfer;
import com.wfee.systems.EnergyTickSystem;

import javax.annotation.Nonnull;

public class EnergyModule extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private ComponentType<EntityStore, EnergyNode> energyNodeComponentType;
    private ComponentType<EntityStore, EnergyTransfer> energyTransferComponentType;
    private static EnergyModule instance;

    public EnergyModule(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
        this.energyNodeComponentType = this.getEntityStoreRegistry().registerComponent(EnergyNode.class, "Enertalic:EnergyNode", EnergyNode.CODEC);
        this.energyTransferComponentType = this.getEntityStoreRegistry().registerComponent(EnergyTransfer.class, "Enertalic:EnergyTransfer", EnergyTransfer.CODEC);
        this.getEntityStoreRegistry().registerSystem(new EnergyTickSystem());
        LOGGER.atInfo().log("EnergySystem was successfully loaded!");
    }

    public static EnergyModule get() {
        return instance;
    }

    public ComponentType<EntityStore, EnergyNode> getEnergyNodeComponentType() {
        return energyNodeComponentType;
    }

    public ComponentType<EntityStore, EnergyTransfer> getEnergyTransferComponentType() {
        return energyTransferComponentType;
    }
}
