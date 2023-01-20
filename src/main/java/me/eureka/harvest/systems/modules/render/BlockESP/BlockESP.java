package me.eureka.harvest.systems.modules.render.BlockESP;

import me.eureka.harvest.events.event.Render3DEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.ic.util.Renderer3D;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.utils.advanced.BlockPosX;
import com.google.common.eventbus.Subscribe;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Module.Info(name = "BlockESP", category = Module.Category.Render)
public class BlockESP extends Module {
    public Setting<String> mode = register("Mode", List.of("Fill", "Outline", "Both"), "Outline");
    public Setting<Boolean> chest = register("Chest", true);
    public Setting<Boolean> eChest = register("EChest", true);
    public Setting<Boolean> shulker = register("Shulker", true);
    public Setting<Boolean> endPortal = register("EndPortal", true);
    public Setting<Boolean> tracers = register("Tracers", false);
    public Setting<Boolean> white = register("White", false);

    @Subscribe
    public void onRender(Render3DEvent event) {
        int count = 0;

        for (BlockEntity blockEntity : getBlockEntities()) {
            if (shouldRender(blockEntity)) {
                render(event, blockEntity.getPos(), tracers.get());
                count++;
            }
        }

        setDisplayInfo(String.valueOf(count));
    }

    private boolean shouldRender(BlockEntity blockEntity) {
        if (blockEntity instanceof ChestBlockEntity) return chest.get();
        if (blockEntity instanceof EnderChestBlockEntity) return eChest.get();
        if (blockEntity instanceof ShulkerBoxBlockEntity) return shulker.get();
        if (blockEntity instanceof EndPortalBlockEntity) return endPortal.get();

        return false;
    }

    private void render(Render3DEvent event, BlockPos blockPos, boolean tracers) {
        BlockPosX blockPosX = new BlockPosX(blockPos);

        Vec3d vec3d = Renderer3D.getRenderPosition(blockPosX.vec3d());
        Box box = new Box(vec3d.x, vec3d.y, vec3d.z, vec3d.x + 1, vec3d.y + 1, vec3d.z + 1);

        int[] color = getColorForBlock(blockPosX);
        Color c = new Color(color[0], color[1], color[2], 255);

        switch (mode.get()) {
            case "Fill" -> Renderer3D.get.drawFilledBox(event.matrix(), box, c.getRGB());
            case "Outline" -> Renderer3D.get.drawOutlineBox(event.matrix(), box, c.getRGB());
            case "Both" -> {
                Renderer3D.get.drawBox(event.matrix(), box, c.getRGB());
                Renderer3D.get.drawOutlineBox(event.matrix(), box, c.getRGB());
            }
        }

        if (!tracers) return;
        renderTracer(event, blockPosX, white.get() ? Color.WHITE : c);
    }

    private void renderTracer(Render3DEvent event, BlockPosX blockPos, Color color) {
        if (mc.crosshairTarget == null) return;
        Vec3d vec3d = Renderer3D.getRenderPosition(blockPos.center());
        Vec3d player = Renderer3D.getRenderPosition(mc.crosshairTarget.getPos());

        Renderer3D.drawLine(event.matrix(), (float) vec3d.x, (float) vec3d.y, (float) vec3d.z, (float) player.x, (float) player.y, (float) player.z, color.getRGB());
    }


    private List<BlockEntity> getBlockEntities() {
        List<BlockEntity> blockEntities = new ArrayList<>();
        ChunkPos chunkPos = mc.player.getChunkPos();
        int viewDistance = mc.options.getViewDistance().getValue();
        for (int x = -viewDistance; x <= viewDistance; x++) {
            for (int z = -viewDistance; z <= viewDistance; z++) {
                WorldChunk worldChunk = mc.world.getChunkManager().getWorldChunk(chunkPos.x + x, chunkPos.z + z);
                if (worldChunk != null) blockEntities.addAll(worldChunk.getBlockEntities().values());
            }
        }

        return blockEntities;

    }

    public int[] getColorForBlock(BlockPosX bp) {
        if (bp.of(Blocks.NETHER_PORTAL)) {
            return new int[]{107, 0, 209};
        }

        int color = bp.state().getMapColor(mc.world, bp.get()).color;
        return new int[]{(color & 0xff0000) >> 16, (color & 0xff00) >> 8, color & 0xff};
    }
}
