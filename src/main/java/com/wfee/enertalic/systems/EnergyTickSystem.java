package com.wfee.enertalic.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktick.BlockTickStrategy;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.ChunkSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.wfee.enertalic.components.EnergyNode;
import com.wfee.enertalic.components.EnergyTransfer;
import com.wfee.enertalic.data.EnergySideConfig;
import com.wfee.enertalic.util.Direction;
import com.wfee.enertalic.util.EnergyTraversal;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class EnergyTickSystem extends EntityTickingSystem<ChunkStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

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

                EnergyNode node = commandBuffer1.getComponent(blockRef, EnergyNode.getComponentType());

                if (node == null) {
                    return BlockTickStrategy.IGNORED;
                }

                WorldChunk chunk = commandBuffer.getComponent(section.getChunkColumnReference(), WorldChunk.getComponentType());

                if (chunk == null) {
                    return BlockTickStrategy.IGNORED;
                }

                World world = chunk.getWorld();

                if (world == null) {
                    return BlockTickStrategy.IGNORED;
                }

                computeNode(
                        node,
                        localX + chunk.getX() * 32,
                        localY,
                        localZ + chunk.getZ() * 32,
                        dt,
                        world,
                        chunk
                );

                return BlockTickStrategy.CONTINUE;
            });
        }
    }

    private void computeNode(
            EnergyNode node,
            int x,
            int y,
            int z,
            double dt,
            World world,
            WorldChunk chunk) {
        Set<Vector3i> seenBlocks = new HashSet<>();
        Vector3i position = new Vector3i(x, y, z);

        seenBlocks.add(position);

        EnergyTraversal path = getPathToSingleImport(position, seenBlocks, node.getEnergySideConfig(), world);

        if (path == null) {
            return;
        }

        double speed = getSpeed(node, dt, path);

        node.removeEnergy((long)speed);
        path.destination().addEnergy((long)speed);

        for (EnergyTransfer transfer : path.path()) {
            transfer.setCurrentTransferRate((long)speed);
        }
        
        chunk.markNeedsSaving();
    }

    private static double getSpeed(EnergyNode node, double dt, EnergyTraversal path) {
        long maxTransferRate = 0;

        for (EnergyTransfer transfer : path.path()) {
            if (maxTransferRate < transfer.getMaxTransferRate()) {
                maxTransferRate = transfer.getMaxTransferRate();
            }
        }

        double speed = maxTransferRate * dt;

        if (speed > node.getCurrentEnergy()) {
            speed = node.getCurrentEnergy();
        }

        long freeSpace = path.destination().getMaxEnergy() - path.destination().getCurrentEnergy();

        if (speed > freeSpace) {
            speed = freeSpace;
        }
        return speed;
    }

    private EnergyTraversal getConnection(
            Vector3i position,
            Set<Vector3i> seenBlocks,
            Direction direction,
            World world) {
        if (!seenBlocks.add(position)) {
            return null;
        }

        Holder<ChunkStore> holder = world.getBlockComponentHolder(position.x, position.y, position.z);

        if (holder == null)
        {
            return null;
        }

        EnergyNode node = holder.getComponent(EnergyNode.getComponentType());

        if (node != null && node.getEnergySideConfig().getDirection(direction).canImport()) {
            return new EnergyTraversal(node, new ArrayList<>());
        }

        EnergyTransfer transfer = holder.getComponent(EnergyTransfer.getComponentType());

        if (transfer == null) {
            return null;
        }

        EnergyTraversal path = getPathToSingleImport(position, seenBlocks, transfer.getEnergySideConfig(), world);

        if (path != null) {
            path.path().add(transfer);
        }

        return path;
    }

    private EnergyTraversal getPathToSingleImport(
            Vector3i position,
            Set<Vector3i> seenBlocks,
            EnergySideConfig energySideConfig,
            World world) {
        EnergyTraversal path;

        for (Direction direction : Direction.values()) {
            if (energySideConfig.getDirection(direction).canExport()) {
                path = getConnection(position.add(direction.getOffset()), seenBlocks, direction.getOpposite(), world);

                if (path != null) {
                    return path;
                }
            }
        }

        return null;
    }

    @Nullable
    @Override
    public Query<ChunkStore> getQuery() {
        return query;
    }
}
