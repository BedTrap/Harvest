package me.eureka.harvest.systems.modules.render.Search;

import me.eureka.harvest.events.event.PacketEvent;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

import static me.eureka.harvest.ic.util.Wrapper.mc;

public class ChunkProcessor {
    private ExecutorService executor;

    private int threads;
    private BiConsumer<ChunkPos, WorldChunk> loadChunkConsumer;
    private BiConsumer<ChunkPos, WorldChunk> unloadChunkConsumer;
    private BiConsumer<BlockPos, BlockState> updateBlockConsumer;

    public ChunkProcessor(int threads, BiConsumer<ChunkPos, WorldChunk> loadChunkConsumer, BiConsumer<ChunkPos, WorldChunk> unloadChunkConsumer, BiConsumer<BlockPos, BlockState> updateBlockConsumer) {
        this.threads = threads;
        this.loadChunkConsumer = loadChunkConsumer;
        this.unloadChunkConsumer = unloadChunkConsumer;
        this.updateBlockConsumer = updateBlockConsumer;
    }

    public void start() {
        executor = Executors.newFixedThreadPool(threads);
    }

    public void stop() {
        executor.shutdownNow();
        executor = null;
    }

    public void restartExecutor() {
        executor.shutdownNow();
        executor = Executors.newFixedThreadPool(threads);
    }

    public void submitAllLoadedChunks() {
        if (loadChunkConsumer != null) {
            for (WorldChunk chunk : getLoadedChunks()) {
                executor.execute(() -> loadChunkConsumer.accept(chunk.getPos(), chunk));
            }
        }
    }

    public void onPacket(PacketEvent.Receive event) {
        if (MinecraftClient.getInstance().world == null)
            return;

        if (updateBlockConsumer != null && event.packet instanceof BlockUpdateS2CPacket) {
            BlockUpdateS2CPacket packet = (BlockUpdateS2CPacket) event.packet;

            executor.execute(() -> updateBlockConsumer.accept(packet.getPos(), packet.getState()));
        } else if (updateBlockConsumer != null && event.packet instanceof ExplosionS2CPacket) {
            ExplosionS2CPacket packet = (ExplosionS2CPacket) event.packet;

            for (BlockPos pos : packet.getAffectedBlocks()) {
                executor.execute(() -> updateBlockConsumer.accept(pos, Blocks.AIR.getDefaultState()));
            }
        } else if (updateBlockConsumer != null && event.packet instanceof ChunkDeltaUpdateS2CPacket) {
            ChunkDeltaUpdateS2CPacket packet = (ChunkDeltaUpdateS2CPacket) event.packet;

            packet.visitUpdates((pos, state) -> {
                BlockPos impos/*ter*/ = pos.toImmutable();
                executor.execute(() -> updateBlockConsumer.accept(impos, state));
            });
        } else if (loadChunkConsumer != null && event.packet instanceof ChunkDataS2CPacket) {
            ChunkDataS2CPacket packet = (ChunkDataS2CPacket) event.packet;

            ChunkPos cp = new ChunkPos(packet.getX(), packet.getZ());
            WorldChunk chunk = new WorldChunk(MinecraftClient.getInstance().world, cp);
            chunk.loadFromPacket(packet.getChunkData().getSectionsDataBuf(), new NbtCompound(), packet.getChunkData().getBlockEntities(packet.getX(), packet.getZ()));

            executor.execute(() -> loadChunkConsumer.accept(cp, chunk));
        } else if (unloadChunkConsumer != null && event.packet instanceof UnloadChunkS2CPacket) {
            UnloadChunkS2CPacket packet = (UnloadChunkS2CPacket) event.packet;

            ChunkPos cp = new ChunkPos(packet.getX(), packet.getZ());
            WorldChunk chunk = MinecraftClient.getInstance().world.getChunk(cp.x, cp.z);

            executor.execute(() -> unloadChunkConsumer.accept(cp, chunk));
        }
    }

    public List<WorldChunk> getLoadedChunks() {
        List<WorldChunk> chunks = new ArrayList<>();

        int viewDist = mc.options.getViewDistance().getValue();

        for (int x = -viewDist; x <= viewDist; x++) {
            for (int z = -viewDist; z <= viewDist; z++) {
                WorldChunk chunk = mc.world.getChunkManager().getWorldChunk((int) mc.player.getX() / 16 + x, (int) mc.player.getZ() / 16 + z);

                if (chunk != null) {
                    chunks.add(chunk);
                }
            }
        }

        return chunks;
    }
}