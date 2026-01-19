package com.wfee;

import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.wfee.components.EnergyNode;
import com.wfee.components.EnergyTransfer;
import com.wfee.systems.EnergyTickSystem;

import javax.annotation.Nonnull;

public class Enertalic extends JavaPlugin {
    private ComponentType<ChunkStore, EnergyNode> energyNodeComponentType;
    private ComponentType<ChunkStore, EnergyTransfer> energyTransferComponentType;
    private static Enertalic instance;

    public Enertalic(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
        super.setup();
        ComponentRegistryProxy<ChunkStore> chunkStoreRegistry = this.getChunkStoreRegistry();
        this.energyNodeComponentType = chunkStoreRegistry.registerComponent(EnergyNode.class, "Enertalic:EnergyNode", EnergyNode.CODEC);
        this.energyTransferComponentType = chunkStoreRegistry.registerComponent(EnergyTransfer.class, "Enertalic:EnergyTransfer", EnergyTransfer.CODEC);
        chunkStoreRegistry.registerSystem(new EnergyTickSystem());
    }

    @Override
    protected void start() {
    }

    public static Enertalic get() {
        return instance;
    }

    public ComponentType<ChunkStore, EnergyNode> getEnergyNodeComponentType() {
        return energyNodeComponentType;
    }

    public ComponentType<ChunkStore, EnergyTransfer> getEnergyTransferComponentType() {
        return energyTransferComponentType;
    }
}