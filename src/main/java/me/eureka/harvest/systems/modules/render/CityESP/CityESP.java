package me.eureka.harvest.systems.modules.render.CityESP;

import com.google.common.eventbus.Subscribe;
import me.eureka.harvest.Hack;
import me.eureka.harvest.events.event.Render3DEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.ic.util.Renderer3D;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.modules.client.HUD.HUD;
import me.eureka.harvest.systems.utils.advanced.BlockPosX;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Module.Info(name = "CityESP", category = Module.Category.Render)
public class CityESP extends Module {
    public Setting<Double> range = register("Range", 5, 0, 10, 2);
    public Setting<Boolean> placement = register("Placement", false);
    public Setting<String> shapeMode = register("ShapeMode", List.of("Line", "Fill", "Both"), "Both");
    public Setting<Integer> red = register("Red", 200, 0, 255);
    public Setting<Integer> green = register("Green", 0, 0, 255);
    public Setting<Integer> blue = register("Blue", 0, 0, 255);
    public Setting<Integer> alpha = register("Alpha", 140, 0, 255);

    private List<BlockPosX> possiblePoses(BlockPosX bp) {
        List<BlockPosX> poses = new ArrayList<>();
        BlockPos.iterate(bp.down().north().west(), bp.south().east()).forEach(bpx -> poses.add(new BlockPosX(bpx)));

        return poses.stream().filter(bpx -> {
            if (!bpx.air()) return false;
            if (!(bpx.down().of(Blocks.OBSIDIAN) || bpx.down().of(Blocks.BEDROCK))) return false;
            if (bpx.hasEntity(entity -> !(entity instanceof EndCrystalEntity))) return false;

            return bpx.distance() < range.get();
        }).toList();
    }

    private List<BlockPosX> surround(PlayerEntity player) {
        List<BlockPosX> poses = new ArrayList<>();
        BlockPosX bp = new BlockPosX(player.getPos());

        if (intersects(player.getBoundingBox())) return null;
        for (Direction direction : Direction.values()) {
            if (direction == Direction.UP || direction == Direction.DOWN) continue;

            if (bp.offset(direction).air()) return null;
            poses.add(bp.offset(direction));
        }

        return poses.stream().filter(bpx -> bpx.blastRes() && bpx.breakable()).toList();
    }

    private boolean intersects(Box box) {
        for (int x = (int) Math.floor(box.minX); x < Math.ceil(box.maxX); x++) {
            for (int y = (int) Math.floor(box.minY); y < Math.ceil(box.maxY); y++) {
                for (int z = (int) Math.floor(box.minZ); z < Math.ceil(box.maxZ); z++) {
                    BlockPosX bp = new BlockPosX(x, y, z);
                    if (bp.blastRes()) return true;
                }
            }
        }

        return false;
    }

    @Subscribe
    public void onRender(Render3DEvent event) {
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (mc.player == player) continue;
            if (Hack.friends().isFriend(player)) continue;
            if (mc.player.distanceTo(player) > range.get() + (range.get() / 2)) continue;

            List<BlockPosX> surround = surround(player);
            if (surround == null) continue;
            if (surround.isEmpty()) continue;

            AtomicReference<BlockPosX> cityPos = new AtomicReference<>();
            AtomicReference<BlockPosX> crystalPos = new AtomicReference<>();
            AtomicReference<Double> distance = new AtomicReference<>(420.0);

            for (BlockPosX bp : surround) {
                List<BlockPosX> possiblePoses = possiblePoses(bp);
                if (possiblePoses.isEmpty()) continue;

                possiblePoses = possiblePoses.stream().filter(bpx -> {
                    Vec3d v = new Vec3d(bpx.getX(), bpx.getY(), bpx.getZ());
                    Box endCrystal = new Box(v.x - 0.5, v.y, v.z - 0.5, v.x + 1.5, v.y + 2, v.z + 1.5);

                    return endCrystal.intersects(bp.box());
                }).toList();
                if (possiblePoses.isEmpty()) continue;

                possiblePoses.forEach(bpx -> {
                    if (bpx.distance() < distance.get()) {
                        cityPos.set(bp);
                        crystalPos.set(bpx);
                        distance.set(bpx.distance());
                    }
                });
            }

            if (cityPos.get() == null) continue;
            render(event, cityPos.get(), false);
            if (!placement.get() || crystalPos.get() == null) continue;
            render(event, crystalPos.get(), true);
        }
    }

    private void render(Render3DEvent event, BlockPosX bp, boolean placement) {
        Vec3d v = Renderer3D.getRenderPosition(bp.get());
        Box box = new Box(v.getX(), v.getY(), v.getZ(), v.getX() + 1, v.getY() + (placement ? 0.01 : 1.0), v.getZ() + 1);
        int color = new Color(red.get(), green.get(), blue.get(), alpha.get()).getRGB();

        switch (shapeMode.get()) {
            case "Line" -> Renderer3D.get.drawOutlineBox(event.matrix(), box, color);
            case "Fill" -> Renderer3D.get.drawBox(event.matrix(), box, color);
            case "Both" -> {
                Renderer3D.get.drawBox(event.matrix(), box, color);
                Renderer3D.get.drawOutlineBox(event.matrix(), box, color);
            }
        }
    }
}
