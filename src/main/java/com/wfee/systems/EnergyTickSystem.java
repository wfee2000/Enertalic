package com.wfee.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktick.BlockTickStrategy;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.ChunkSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.wfee.components.EnergyNode;
import com.wfee.components.EnergyTransfer;
import com.wfee.util.Position;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

public class EnergyTickSystem extends EntityTickingSystem<ChunkStore> {
    @Nonnull
    private final Query<ChunkStore> query;

    public EnergyTickSystem() {
        this.query = Query.and(BlockSection.getComponentType(), ChunkSection.getComponentType());
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<ChunkStore> archetypeChunk, @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
        BlockSection blocks = archetypeChunk.getComponent(index, BlockSection.getComponentType());

        assert blocks != null;

        if (blocks.getTickingBlocksCountCopy() != 0) {
            ChunkSection section = archetypeChunk.getComponent(index, ChunkSection.getComponentType());

            assert section != null;

            BlockComponentChunk blockComponentChunk = commandBuffer.getComponent(section.getChunkColumnReference(), BlockComponentChunk.getComponentType());

            assert blockComponentChunk != null;

            blocks.forEachTicking(blockComponentChunk, commandBuffer, section.getY(), (blockComponentChunk1, commandBuffer1, localX, localY, localZ, _) ->
            {
                Ref<ChunkStore> blockRef = blockComponentChunk1.getEntityReference(ChunkUtil.indexBlockInColumn(localX, localY, localZ));

                if (blockRef == null) {
                    return BlockTickStrategy.IGNORED;
                }

                EnergyNode exampleBlock = commandBuffer1.getComponent(blockRef, EnergyNode.getComponentType());

                if (exampleBlock == null) {
                    return BlockTickStrategy.IGNORED;
                }

                WorldChunk worldChunk = commandBuffer.getComponent(section.getChunkColumnReference(), WorldChunk.getComponentType());

                if (worldChunk == null) {
                    return BlockTickStrategy.IGNORED;
                }

                Set<Position> seenBlocks = new HashSet<>();
                seenBlocks.add(new Position(localX, localY, localZ));

                if (checkSurroundingBlocksForNode(localX, localY, localZ, commandBuffer1, blockComponentChunk1, seenBlocks)) {
                    // TODO: transfer Energy
                }

                return BlockTickStrategy.CONTINUE;
            });
        }
    }

    @Nullable
    @Override
    public Query<ChunkStore> getQuery() {
        return query;
    }
}
