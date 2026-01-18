package com.wfee.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.wfee.components.EnergyNode;
import com.wfee.components.EnergyTransfer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EnergyTickSystem extends EntityTickingSystem<EntityStore> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Nonnull
    private final Query<EntityStore> query;

    public EnergyTickSystem() {
        this.query = Query.and(EnergyNode.getComponentType(), EnergyTransfer.getComponentType());
    }

    @Override
    public void tick(float v, int i, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        LOGGER.atInfo().log("EnergySystem Ticked");
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }
}
