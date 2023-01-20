package me.eureka.harvest.systems.modules.render.Search;

import me.eureka.harvest.events.event.PacketEvent;
import me.eureka.harvest.events.event.Render3DEvent;
import me.eureka.harvest.events.event.TickEvent;
import me.eureka.harvest.ic.util.Renderer3D;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.utils.advanced.BlockPosX;
import com.google.common.collect.Sets;
import com.google.common.eventbus.Subscribe;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.s2c.play.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Module.Info(name = "Search", category = Module.Category.Render)
public class Search extends Module {
    private final List<Block> list = List.of(Blocks.DIAMOND_ORE, Blocks.ANCIENT_DEBRIS);
    private Set<BlockPos> foundBlocks = Sets.newConcurrentHashSet();
    private ChunkProcessor processor = new ChunkProcessor(1,
            (cp, chunk) -> {
                if (foundBlocks.size() > 1000000)
                    return;

                for (int x = 0; x < 16; x++) {
                    for (int y = mc.world.getBottomY(); y < mc.world.getTopY(); y++) {
                        for (int z = 0; z < 16; z++) {
                            BlockPos pos = new BlockPos(cp.getStartX() + x, y, cp.getStartZ() + z);
                            BlockState state = chunk.getBlockState(pos);
                            if (list.contains(state.getBlock())) {
                                foundBlocks.add(pos);
                            }
                        }
                    }
                }
            },
            (cp, chunk) ->
                    foundBlocks.removeIf(pos -> pos.getX() >= cp.getStartX() && pos.getX() <= cp.getEndX() && pos.getZ() >= cp.getStartZ() && pos.getZ() <= cp.getEndZ()),
            (pos, state) -> {
                if (list.contains(state.getBlock())) {
                    foundBlocks.add(pos);
                } else {
                    foundBlocks.remove(pos);
                }
            });

    private Set<Block> prevBlockList = new HashSet<>();
    private int oldViewDistance = -1;

    @Override
    public void onDeactivate() {
        foundBlocks.clear();
        prevBlockList.clear();
        processor.stop();
    }

    @Override
    public void onActivate() {
        processor.start();
    }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        Set<Block> blockList = Set.of(list.get(0), list.get(1));

        if (!prevBlockList.equals(blockList) || oldViewDistance != mc.options.getViewDistance().getValue()) {
            foundBlocks.clear();

            processor.submitAllLoadedChunks();

            prevBlockList = new HashSet<>(blockList);
            oldViewDistance = mc.options.getViewDistance().getValue();
        }
    }

    @Subscribe
    public void onReadPacket(PacketEvent.Receive event) {
        processor.onPacket(event);

        if (event.packet instanceof DisconnectS2CPacket
                || event.packet instanceof GameJoinS2CPacket
                || event.packet instanceof PlayerRespawnS2CPacket) {
            foundBlocks.clear();
            prevBlockList.clear();
            processor.restartExecutor();
        }
    }

    @Subscribe
    public void onRender(Render3DEvent event) {
        int i = 0;
        for (BlockPos pos : foundBlocks) {
            if (i > 3000) return;
            BlockState state = mc.world.getBlockState(pos);

            int[] color = getColorForBlock(state, pos);
            Color c = new Color(color[0], color[1], color[2], 255);

            Vec3d vec3d = Renderer3D.getRenderPosition(new BlockPosX(pos).vec3d());
            Box box = new Box(vec3d.x, vec3d.y, vec3d.z, vec3d.x + 1, vec3d.y + 1, vec3d.z + 1);

            Renderer3D.get.drawBox(event.matrix(), box, c.getRGB());
            Renderer3D.get.drawOutlineBox(event.matrix(), box, c.getRGB());

            i++;
        }
    }

    public int[] getColorForBlock(BlockState state, BlockPos pos) {
        if (state.getBlock() == Blocks.NETHER_PORTAL) {
            return new int[]{107, 0, 209};
        }

        int color = state.getMapColor(mc.world, pos).color;
        return new int[]{(color & 0xff0000) >> 16, (color & 0xff00) >> 8, color & 0xff};
    }
}
