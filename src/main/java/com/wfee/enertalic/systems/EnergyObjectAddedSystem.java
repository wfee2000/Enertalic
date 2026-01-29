package com.wfee.enertalic.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.wfee.enertalic.components.EnergyNode;
import com.wfee.enertalic.components.EnergyObject;
import com.wfee.enertalic.components.EnergyTransfer;
import com.wfee.enertalic.data.network.NetworkService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EnergyObjectAddedSystem extends RefSystem<ChunkStore> {
    private final static HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final Query<ChunkStore> query;

    public EnergyObjectAddedSystem() {
        this.query = Query.or(
                EnergyNode.getComponentType(),
                EnergyTransfer.getComponentType()
        );
    }

    @Override
    public void onEntityAdded(@Nonnull Ref<ChunkStore> ref, @Nonnull AddReason addReason, @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
        var info = store.getComponent(ref, BlockModule.BlockStateInfo.getComponentType());
        if (info == null) {
            return;
        }

        EnergyObject object = commandBuffer.getComponent(ref, EnergyNode.getComponentType());

        if (object == null) {
            object = commandBuffer.getComponent(ref, EnergyTransfer.getComponentType());
        }

        if (object == null) {
            return;
        }

        int x = ChunkUtil.xFromBlockInColumn(info.getIndex());
        int y = ChunkUtil.yFromBlockInColumn(info.getIndex());
        int z = ChunkUtil.zFromBlockInColumn(info.getIndex());

        WorldChunk worldChunk = commandBuffer.getComponent(info.getChunkRef(), WorldChunk.getComponentType());

        if (worldChunk == null) {
            return;
        }

        worldChunk.setTicking(x, y, z, true);

        NetworkService.getInstance().addNewObject(
                object,
                x + worldChunk.getX() * 32,
                y,
                z + worldChunk.getZ() * 32,
                worldChunk.getWorld()
        );
    }

    @Override
    public void onEntityRemove(@Nonnull Ref<ChunkStore> ref, @Nonnull RemoveReason removeReason, @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
        BlockModule.BlockStateInfo info = commandBuffer.getComponent(ref, BlockModule.BlockStateInfo.getComponentType());
        if (info == null) return;

        EnergyObject object = commandBuffer.getComponent(ref, EnergyNode.getComponentType());
        NetworkService.getInstance().removeObject(object);
    }


    @Nullable
    @Override
    public Query<ChunkStore> getQuery() {
        return this.query;
    }
}
