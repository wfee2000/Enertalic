package com.wfee.enertalic.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktick.BlockTickStrategy;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.ChunkSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.wfee.enertalic.components.EnergyBase;
import com.wfee.enertalic.components.EnergyNode;
import com.wfee.enertalic.components.EnergyTransfer;
import com.wfee.enertalic.data.EnergySideConfig;
import com.wfee.enertalic.util.Direction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

                Set<Vector3i> seenBlocks = new HashSet<>();
                Vector3i position = new Vector3i(localX, localY, localZ);

                seenBlocks.add(position);

                List<EnergyBase> path = getPathToSingleImport(position, commandBuffer1, blockComponentChunk1, seenBlocks, node.getEnergySideConfig());

                if (path != null) {
                    EnergyNode destinationNode = null;
                    long maxTransferRate = 0;

                    for (EnergyBase energyBase : path) {
                        if (energyBase instanceof EnergyNode pathNode) {
                            destinationNode = pathNode;
                        } else if (energyBase instanceof EnergyTransfer energyTransfer) {
                            if (maxTransferRate < energyTransfer.getMaxTransferRate()) {
                                maxTransferRate = energyTransfer.getMaxTransferRate();
                            }
                        } else {
                            throw new IllegalArgumentException();
                        }
                    }

                    if (destinationNode == null) {
                        throw new IllegalStateException();
                    }

                    double speed = maxTransferRate * dt;

                    if (speed > node.getCurrentEnergy()) {
                        speed = node.getCurrentEnergy();
                    }

                    long freeSpace = destinationNode.getMaxEnergy() - destinationNode.getCurrentEnergy();

                    if (speed > freeSpace) {
                        speed = freeSpace;
                    }

                    node.removeEnergy((long)speed);
                    destinationNode.addEnergy((long)speed);

                    for (EnergyBase energyBase : path) {
                        if (energyBase instanceof EnergyTransfer energyTransfer) {
                            energyTransfer.setCurrentTransferRate((long)speed);
                        } else if (!(energyBase instanceof EnergyNode)) {
                            throw new IllegalArgumentException();
                        }
                    }
                }

                return BlockTickStrategy.CONTINUE;
            });
        }
    }

    private List<EnergyBase> getConnection(Vector3i position, CommandBuffer<ChunkStore> commandBuffer, BlockComponentChunk blockComponentChunk, Set<Vector3i> seenBlocks, Direction direction) {
        if (!seenBlocks.add(position)) {
            return null;
        }

        Ref<ChunkStore> blockReference = blockComponentChunk.getEntityReference(ChunkUtil.indexBlockInColumn(position.x, position.y, position.z));

        if (blockReference == null) {
            return null;
        }

        EnergyNode node = commandBuffer.getComponent(blockReference, EnergyNode.getComponentType());

        if (node != null && node.getEnergySideConfig().getDirection(direction).canImport()) {
            return new ArrayList<>(List.of(node));
        }

        EnergyTransfer transfer = commandBuffer.getComponent(blockReference, EnergyTransfer.getComponentType());

        if (transfer == null) {
            return null;
        }

        List<EnergyBase> path = getPathToSingleImport(position, commandBuffer, blockComponentChunk, seenBlocks, transfer.getEnergySideConfig());

        if (path != null) {
            path.add(transfer);
        }

        return path;
    }

    private List<EnergyBase> getPathToSingleImport(
            Vector3i position,
            CommandBuffer<ChunkStore> commandBuffer,
            BlockComponentChunk blockComponentChunk,
            Set<Vector3i> seenBlocks,
            EnergySideConfig energySideConfig
    ) {
        List<EnergyBase> path;

        for (Direction direction : Direction.values()) {
            if (energySideConfig.getDirection(direction).canExport()) {
                path = getConnection(position.add(direction.getOffset()), commandBuffer, blockComponentChunk, seenBlocks, direction.getOpposite());

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
