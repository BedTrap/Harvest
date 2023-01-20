package me.eureka.harvest.systems.modules.combat.AutoCity;

import com.google.common.eventbus.Subscribe;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import me.eureka.harvest.Hack;
import me.eureka.harvest.events.event.Render3DEvent;
import me.eureka.harvest.events.event.StartBreakingBlockEvent;
import me.eureka.harvest.events.event.TickEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.ic.util.Draw3D;
import me.eureka.harvest.ic.util.Renderer3D;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.modules.world.PacketMine.PacketMine;
import me.eureka.harvest.systems.utils.advanced.BlockPosX;
import me.eureka.harvest.systems.utils.advanced.FindItemResult;
import me.eureka.harvest.systems.utils.advanced.InvUtils;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Module.Info(name = "AutoCity", category = Module.Category.Combat)
public class AutoCity extends Module {
    public Setting<String> swap = register("Swap", List.of("Normal", "Silent", "Move"), "Silent");
    public Setting<Boolean> inventory = register("Inventory", false);
    public Setting<Boolean> preSwing = register("PreSwing", true);
    public Setting<Boolean> postSwing = register("PostSwing", true);
    public Setting<Double> range = register("Range", 5, 0, 7, 2);
    public Setting<String> render = register("Render", List.of("Box", "Percentage", "Both", "OFF"), "Box");
    public Setting<String> shapeMode = register("ShapeMode", List.of("Line", "Fill", "Both"), "Both");
    public Setting<Integer> red = register("Red", 200, 0, 255);
    public Setting<Integer> green = register("Green", 0, 0, 255);
    public Setting<Integer> blue = register("Blue", 0, 0, 255);

    private BlockPosX bp;
    private final PacketUtils packet = new PacketUtils();

    @Subscribe
    public void onTick(TickEvent.Post event) {
        if (bp == null) bp = getPosition();
        if (bp == null) {
            packet.reset();
            return;
        }

        if (!packet.canBreak(bp, range.get())) {
            bp = null;
            packet.reset();
        }
        if (bp == null) return;

        packet.mine(bp,
                () -> {
            if (preSwing.get()) mc.player.swingHand(Hand.MAIN_HAND);
        },
                () -> {
            if (postSwing.get()) mc.player.swingHand(Hand.MAIN_HAND);
        }, swap, inventory);
    }

    @Subscribe
    public void onRender(Render3DEvent event) {
        if (this.bp == null || this.bp.air()) return;

        render(event, this.bp);
    }

    private void render(Render3DEvent event, BlockPosX bp) {
        Vec3d v = Renderer3D.getRenderPosition(bp.get());
        Box box = new Box(v.getX(), v.getY(), v.getZ(), v.getX() + 1, v.getY() + 1.0, v.getZ() + 1);
        int alpha = Integer.parseInt(String.valueOf((packet.progress() > 0.5 ? 1.0 - packet.progress() : packet.progress()) * 255).split("\\.")[0]);
        Color color = new Color(red.get(), green.get(), blue.get(), alpha);

        switch (render.get()) {
            case "Box" -> {
                switch (shapeMode.get()) {
                    case "Line" -> Draw3D.drawOutlineBox(event, box, color);
                    case "Fill" -> Draw3D.drawFilledBox(event, box, color);
                    case "Both" -> Draw3D.drawBox(event, box, color);
                }
            }
            case "Percentage" -> {
                Vec3d r = bp.center();
                Renderer3D.drawText(Text.of(String.format("%.2f", packet.progress()) + "%"), r.x, r.y, r.z, 0.8, false);
            }
            case "Both" -> {
                switch (shapeMode.get()) {
                    case "Line" -> Draw3D.drawOutlineBox(event, box, color);
                    case "Fill" -> Draw3D.drawFilledBox(event, box, color);
                    case "Both" -> Draw3D.drawBox(event, box, color);
                }
                
                Vec3d r = bp.center();
                Renderer3D.drawText(Text.of(String.format("%.2f", packet.progress()) + "%"), r.x, r.y, r.z, 0.8, false);
            }
        }
    }

    private BlockPosX getPosition() {
        List<BlockPosX> positions = new ArrayList<>();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (mc.player == player) continue;
            if (Hack.friends().isFriend(player)) continue;
            if (mc.player.distanceTo(player) > range.get() + (range.get() / 2)) continue;

            List<BlockPosX> surround = surround(player);
            if (surround == null) continue;
            if (surround.isEmpty()) continue;

            AtomicReference<BlockPosX> cityPos = new AtomicReference<>();
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
                        distance.set(bpx.distance());
                    }
                });
            }

            if (cityPos.get() == null) continue;
            positions.add(cityPos.get());
        }

        if (positions.isEmpty()) return null;
        positions.sort(Comparator.comparingDouble(BlockPosX::distance));
        return positions.get(0);
    }

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
}
