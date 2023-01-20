package me.eureka.harvest.systems.modules.combat.FeetTrap;

import me.eureka.harvest.events.event.BlockUpdateEvent;
import me.eureka.harvest.events.event.Render3DEvent;
import me.eureka.harvest.events.event.TickEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.ic.util.Draw3D;
import me.eureka.harvest.ic.util.Renderer3D;
import me.eureka.harvest.mixins.ClientPlayerInteractionManagerAccessor;
import me.eureka.harvest.mixins.WorldRendererAccessor;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.modules.client.HUD.HUD;
import me.eureka.harvest.systems.utils.advanced.BlockPosX;
import me.eureka.harvest.systems.utils.advanced.FindItemResult;
import me.eureka.harvest.systems.utils.advanced.Place;
import me.eureka.harvest.systems.utils.other.TimerUtils;
import com.google.common.eventbus.Subscribe;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.render.BlockBreakingInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static me.eureka.harvest.systems.modules.combat.FeetTrap.FTUtils.*;

@Module.Info(name = "FeetTrap", category = Module.Category.Combat)
public class FeetTrap extends Module {
    public Setting<String> swap = register("Swap", List.of("Normal", "Silent", "Move"), "Silent");
    public Setting<Integer> blockPerInterval = register("BlocksPerInterval", 3, 1, 5);
    public Setting<Integer> intervalDelay = register("IntervalDelay", 1, 0, 3);
    public Setting<Boolean> packet = register("Packet", false);
    //public Setting<Boolean> airStuck = register("AirStuck", false);
    public Setting<Boolean> antiBreak = register("AntiBreak", false);
    public Setting<Boolean> antiCrystal = register("AntiCrystal", false);
    public Setting<Boolean> oldPlace = register("1.12", false);
    public Setting<Boolean> rotate = register("Rotate", false);
    public Setting<Double> collisionPassed = register("CollisionPassed", 1500, 1250, 5000, 0);
    public Setting<Boolean> clientRemove = register("ClientRemove(test)", false);
    public Setting<Boolean> autoDisable = register("AutoDisable", true);
    public Setting<String> shapeMode = register("ShapeMode", List.of("Line", "Fill", "Both"), "Both");
    public Setting<Integer> red = register("Red", 140, 0, 255);
    public Setting<Integer> green = register("Green", 140, 0, 255);
    public Setting<Integer> blue = register("Blue", 140, 0, 255);
    public Setting<Integer> alpha = register("Alpha", 140, 0, 255);

    private List<BlockPos> poses = new ArrayList<>();
    private final List<BlockPos> queue = new ArrayList<>();

    private final TimerUtils unsurroundedTimer = new TimerUtils();
    private final TimerUtils oneTickTimer = new TimerUtils();

    private int interval;

    @Override
    public void onActivate() {
        queue.clear();

        interval = 0;
        unsurroundedTimer.reset();
        oneTickTimer.reset();
    }

    public ArrayList<BlockPos> getPositions(PlayerEntity player) {
        ArrayList<BlockPos> positions = new ArrayList<>();
        List<Entity> getEntityBoxes;

        for (BlockPos blockPos : getSphere(player.getBlockPos(), 3, 1)) {
            if (!mc.world.getBlockState(blockPos).getMaterial().isReplaceable()) continue;
            getEntityBoxes = mc.world.getOtherEntities(null, new Box(blockPos), entity -> entity == player);
            if (!getEntityBoxes.isEmpty()) continue;
            if (unsurroundedTimer.passedMillis(collisionPassed.get().longValue()) && !mc.world.canPlace(Blocks.OBSIDIAN.getDefaultState(), blockPos, ShapeContext.absent()))
                continue;


            for (Direction direction : Direction.values()) {
                if (direction == Direction.UP || direction == Direction.DOWN) continue;

                getEntityBoxes = mc.world.getOtherEntities(null, new Box(blockPos.offset(direction)), entity -> entity == player);
                if (!getEntityBoxes.isEmpty()) positions.add(blockPos);
            }
        }

        if (oldPlace.get()) positions.removeIf(bp -> Place.placeDirection(new BlockPosX(bp)) == null);
        return positions;
    }

    @Subscribe
    public void onUpdate(BlockUpdateEvent event) {
        if (!antiCrystal.get()) return;
        if (!getPositions(mc.player).contains(event.pos)) return;

        List<Entity> entities = mc.world.getOtherEntities(null, new BlockPosX(event.pos).box(), entity -> entity instanceof EndCrystalEntity);
        if (entities.isEmpty()) return;

        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(entities.get(0), mc.player.isSneaking()));
        mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
    }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        if (autoDisable.get() && ((mc.options.jumpKey.isPressed() || mc.player.input.jumping) || mc.player.prevY < mc.player.getPos().getY())) {
            toggle();
            return;
        }

        if (interval > 0) interval--;
        if (interval > 0) return;

        Map<Integer, BlockBreakingInfo> blocks = ((WorldRendererAccessor) mc.worldRenderer).getBlockBreakingInfos();
        BlockPos ownBreakingPos = ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).breakingBlockPos();
        ArrayList<BlockPos> boobies = getSurroundBlocks(mc.player);

        blocks.values().forEach(info -> {
            BlockPos pos = info.getPos();
            if (antiBreak.get() && !pos.equals(ownBreakingPos) && info.getStage() >= 0) {
                if (boobies.contains(pos)) queue.addAll(getBlocksAround(pos));
            }
            if (clientRemove.get() && !pos.equals(ownBreakingPos) && info.getStage() >= 8) {
                if (boobies.contains(pos)) mc.world.setBlockState(pos, Blocks.AIR.getDefaultState());
            }
        });


        poses = getPositions(mc.player);
        poses.addAll(queue);

        setDisplayInfo(String.valueOf(poses.size()));
        FindItemResult block = getBlock();
        if (poses.isEmpty() || !block.found()) {
            if (isSurrounded(mc.player)) unsurroundedTimer.reset();
            return;
        }

        for (int i = 0; i <= blockPerInterval.get(); i++) {
            if (poses.size() > i) {
                BlockPosX bp = new BlockPosX(poses.get(i));

                if (oldPlace.get()) {
                    Place.place(bp, block, Place.by(swap.get()), packet.get(), rotate.get(), true);
                } else place(bp, rotate.get(), block.slot());
                queue.remove(poses.get(i));
            }
        }
        interval = intervalDelay.get();
    }

    private List<BlockPos> getBlocksAround(BlockPos blockPos) {
        List<BlockPos> blocks = new ArrayList<>();

        for (Direction direction : Direction.values()) {
            if (direction == Direction.UP || direction == Direction.DOWN) continue;
            if (hasEntity(new Box(blockPos.offset(direction)))) continue;
            if (!mc.world.isAir(blockPos.offset(direction))) continue;
            if (queue.contains(blockPos.offset(direction))) continue;

            blocks.add(blockPos.offset(direction));
        }

        return blocks;
    }

    @Subscribe
    public void onRender(Render3DEvent event) {
        if (poses.isEmpty()) return;

        poses.forEach(blockPos -> {
            Vec3d vec3d = Renderer3D.getRenderPosition(blockPos.getX(), blockPos.getY(), blockPos.getZ());
            Box box = new Box(vec3d.x, vec3d.y, vec3d.z, vec3d.x + 1, vec3d.y + 1, vec3d.z + 1);
            Color color = new Color(red.get(), green.get(), blue.get(), alpha.get());

            switch (shapeMode.get()) {
                case "Line" -> Draw3D.drawOutlineBox(event, box, color);
                case "Fill" -> Draw3D.drawFilledBox(event, box, color);
                case "Both" -> Draw3D.drawBox(event, box, color);
            }
        });
    }

    public static FeetTrap get;

    public FeetTrap() {
        get = this;
    }
}
