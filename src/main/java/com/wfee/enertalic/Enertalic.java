package com.wfee.enertalic;

import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.wfee.enertalic.components.EnergyNode;
import com.wfee.enertalic.components.EnergyTransfer;
import com.wfee.enertalic.systems.EnergyObjectAddedSystem;
import com.wfee.enertalic.systems.EnergyTickSystem;

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
        chunkStoreRegistry.registerSystem(new EnergyObjectAddedSystem());
        chunkStoreRegistry.registerSystem(new EnergyTickSystem());
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