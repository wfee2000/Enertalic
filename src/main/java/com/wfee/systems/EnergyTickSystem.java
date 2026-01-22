package com.wfee.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktick.BlockTickStrategy;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.ChunkSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.npc.util.NPCPhysicsMath;
import com.wfee.components.EnergyBase;
import com.wfee.components.EnergyNode;
import com.wfee.components.EnergyTransfer;
import com.wfee.data.EnergySideConfig;
import com.wfee.util.Position;

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

                Set<Position> seenBlocks = new HashSet<>();
                seenBlocks.add(new Position(localX, localY, localZ));

                List<EnergyBase> path = getPathToSingleImport(localX, localY, localZ, commandBuffer1, blockComponentChunk1, seenBlocks, node.getEnergySideConfig());

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

    private List<EnergyBase> getConnection(int x, int y, int z, CommandBuffer<ChunkStore> commandBuffer, BlockComponentChunk blockComponentChunk, Set<Position> seenBlocks, NPCPhysicsMath.Direction direction) {
        Position position = new Position(x, y, z);
        if (!seenBlocks.add(position)) {
            return null;
        }

        Ref<ChunkStore> blockReference = blockComponentChunk.getEntityReference(ChunkUtil.indexBlockInColumn(x,y,z));

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

        List<EnergyBase> path = getPathToSingleImport(x, y, z,  commandBuffer, blockComponentChunk, seenBlocks, transfer.getEnergySideConfig());

        if (path != null) {
            path.add(transfer);
        }

        return path;
    }

    private List<EnergyBase> getPathToSingleImport(
            int x,
            int y,
            int z,
            CommandBuffer<ChunkStore> commandBuffer,
            BlockComponentChunk blockComponentChunk,
            Set<Position> seenBlocks,
            EnergySideConfig energySideConfig
    ) {
        List<EnergyBase> path;

        if (energySideConfig.getEast().canExport()) {
            path = getConnection(x + 1, y, z, commandBuffer, blockComponentChunk, seenBlocks, NPCPhysicsMath.Direction.NEG_X);

            if (path != null) {
                return path;
            }
        }

        if (energySideConfig.getWest().canExport()) {
            path = getConnection(x - 1, y, z, commandBuffer, blockComponentChunk, seenBlocks, NPCPhysicsMath.Direction.POS_X);

            if (path != null) {
                return path;
            }
        }

        if (energySideConfig.getUp().canExport()) {
            path = getConnection(x, y + 1, z, commandBuffer, blockComponentChunk, seenBlocks, NPCPhysicsMath.Direction.NEG_Y);

            if (path != null) {
                return path;
            }
        }

        if (energySideConfig.getDown().canExport()) {
            path = getConnection(x, y - 1, z, commandBuffer, blockComponentChunk, seenBlocks,  NPCPhysicsMath.Direction.POS_Y);

            if (path != null) {
                return path;
            }
        }

        if (energySideConfig.getSouth().canExport()) {
            path = getConnection(x, y, z + 1, commandBuffer, blockComponentChunk, seenBlocks, NPCPhysicsMath.Direction.NEG_Z);

            if (path != null) {
                return path;
            }
        }

        if (energySideConfig.getNorth().canExport()) {
            return getConnection(x, y, z - 1, commandBuffer, blockComponentChunk, seenBlocks, NPCPhysicsMath.Direction.POS_Z);
        }

        return null;
    }

    @Nullable
    @Override
    public Query<ChunkStore> getQuery() {
        return query;
    }
}
