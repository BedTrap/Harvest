package me.eureka.harvest.systems.modules.combat.PistonPush;

import me.eureka.harvest.events.event.TickEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.ic.util.RotationUtil;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.utils.advanced.FindItemResult;
import me.eureka.harvest.systems.utils.advanced.InvUtils;
import me.eureka.harvest.systems.utils.advanced.TargetUtils;
import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Comparator;

import static me.eureka.harvest.systems.utils.advanced.InvUtils.findInHotbar;
import static me.eureka.harvest.systems.utils.advanced.TargetUtils.getPlayerTarget;
import static me.eureka.harvest.systems.utils.advanced.TargetUtils.isBadTarget;

@Module.Info(name = "PistonPush", category = Module.Category.Combat)
public class PistonPush extends Module {
    public Setting<Integer> targetRange = register("TargetRange", 6, 0, 8);
    public Setting<Double> placeRange = register("PlaceRange", 6, 0, 8, 1);
    public Setting<Boolean> packet = register("Packet", false);
    public Setting<Boolean> antiSelf = register("AntiSelf", false);
    public Setting<Boolean> reverse = register("Reverse", false);
    public Setting<Boolean> holeFill = register("HoleFill", false);
    public Setting<Boolean> swapBack = register("SwapBack", false);
    public Setting<Boolean> zeroTick = register("ZeroTick", false);
    public Setting<Boolean> eatPause = register("EatPause", false);

    private BlockPos pistonPos, activatorPos, obsidianPos, targetPos;
    private Direction direction;

    private PlayerEntity target;
    private Stage stage;

    private int prevSlot;

    @Override
    public void onActivate() {
        pistonPos = null;
        activatorPos = null;
        obsidianPos = null;

        direction = null;

        stage = Stage.Preparing;
    }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        target = getPlayerTarget(targetRange.get(), TargetUtils.SortPriority.LowestDistance);
        if (isBadTarget(target, targetRange.get())) {
            info("Target is null.");
            toggle();
            return;
        }

        if (!findInHotbar(Items.PISTON, Items.STICKY_PISTON).found() || !findInHotbar(Items.REDSTONE_BLOCK).found()) {
            info("Required items not found.");
            toggle();
            return;
        }

        if (eatPause.get() && mc.player.isUsingItem() && mc.player.getMainHandStack().isFood()) return;

        switch (stage) {
            case Preparing -> {
                prevSlot = mc.player.getInventory().selectedSlot;

                targetPos = target.getBlockPos().up();
                pistonPos = getPistonPos(targetPos);
                activatorPos = getRedstonePos(pistonPos);
                obsidianPos = targetPos.down();

                if (hasNull(targetPos, pistonPos, activatorPos, obsidianPos)) stage = Stage.Toggle;
                if (hasFar(targetPos, pistonPos, activatorPos, obsidianPos)) stage = Stage.Toggle;
                if (antiSelf.get() && PPUtils.hasEntity(new Box(targetPos), entity -> entity == mc.player)) stage = Stage.Toggle;

                stage = zeroTick.get() ? Stage.ZeroTick : (reverse.get() ? Stage.Redstone : Stage.Piston);
            }
            case Piston -> {
                doPlace(findInHotbar(Items.PISTON, Items.STICKY_PISTON), pistonPos);
                stage = reverse.get() ? Stage.Obsidian : Stage.Redstone;
            }
            case Redstone -> {
                doPlace(findInHotbar(Items.REDSTONE_BLOCK), activatorPos);
                stage = reverse.get() ? Stage.Piston : Stage.Obsidian;
            }
            case Obsidian -> {
                if (holeFill.get() && findInHotbar(Items.OBSIDIAN).found()) {
                    doPlace(findInHotbar(Items.OBSIDIAN), obsidianPos);
                }

                stage = Stage.Toggle;
            }
            case ZeroTick -> {
                doPlace(findInHotbar(Items.PISTON, Items.STICKY_PISTON), pistonPos);
                doPlace(findInHotbar(Items.REDSTONE_BLOCK), activatorPos);
                stage = Stage.Toggle;
            }
            case Toggle -> {
                if (swapBack.get()) mc.player.getInventory().selectedSlot = prevSlot;
                toggle();
            }
        }
    }

    private void doPlace(FindItemResult result, BlockPos blockPos) {
        if (blockPos == null) return;
        Hand hand = result.isOffhand() ? Hand.OFF_HAND : Hand.MAIN_HAND;

        RotationUtil.rotate(getYaw(direction), 0);
            InvUtils.swap(result.slot(), false);
            doPlace(hand, new BlockHitResult(PPUtils.closestVec3d(blockPos), Direction.DOWN, blockPos, true), packet.get());
            mc.player.swingHand(hand);
    }

    private void doPlace(Hand hand, BlockHitResult blockHitResult, boolean packet) {
        if (packet) mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(hand, blockHitResult, 0));
        else mc.interactionManager.interactBlock(mc.player, hand, blockHitResult);
    }

    private int getYaw(Direction direction) {
        if (direction == null) return (int) mc.player.getYaw();
        return switch (direction) {
            case NORTH -> 180;
            case SOUTH -> 0;
            case WEST -> 90;
            case EAST -> -90;
            default -> throw new IllegalStateException("Unexpected value: " + direction);
        };
    }

    private Direction revert(Direction direction) {
        return switch (direction) {
            case NORTH -> Direction.SOUTH;
            case SOUTH -> Direction.NORTH;
            case WEST -> Direction.EAST;
            case EAST -> Direction.WEST;
            default -> throw new IllegalStateException("Unexpected value: " + direction);
        };
    }

    private BlockPos getRedstonePos(BlockPos blockPos) {
        ArrayList<BlockPos> pos = new ArrayList<>();
        if (blockPos == null) return null;

        for (Direction dir : Direction.values()) {
            if (PPUtils.hasEntity(new Box(blockPos.offset(dir)))) continue;

            if (canPlace(blockPos.offset(dir))) pos.add(blockPos.offset(dir));
        }

        if (pos.isEmpty()) return null;
        pos.sort(Comparator.comparingDouble(PPUtils::distanceTo));

        return pos.get(0);
    }

    private BlockPos getPistonPos(BlockPos blockPos) {
        ArrayList<BlockPos> pos = new ArrayList<>();

        for (Direction dir : Direction.values()) {
            if (dir == Direction.DOWN || dir == Direction.UP) continue;
            if (PPUtils.hasEntity(new Box(blockPos.offset(dir)))) continue;

            boolean canPush = isAir(blockPos.up()) && isAir(blockPos.offset(revert(dir))) && isAir(blockPos.offset(revert(dir)).up());

            if (canPlace(blockPos.offset(dir)) && canPush) {
                pos.add(blockPos.offset(dir));
            }
        }

        if (pos.isEmpty()) return null;

        pos.sort(Comparator.comparingDouble(PPUtils::distanceTo));
        direction = getDirection(blockPos, pos.get(0));

        return pos.get(0);
    }

    private boolean canPlace(BlockPos blockPos) {
        if (!mc.world.isAir(blockPos)) return false;

        return !PPUtils.hasEntity(new Box(blockPos));
    }

    private Direction getDirection(BlockPos from, BlockPos to) {
        for (Direction dir : Direction.values()) {
            if (dir == Direction.DOWN || dir == Direction.UP) continue;

            if (from.offset(dir).equals(to)) return dir;
        }

        return null;
    }

    private boolean hasNull(BlockPos... blockPoses) {
        for (BlockPos blockPos : blockPoses) {
            if (blockPos == null) return true;
        }

        return false;
    }

    private boolean hasFar(BlockPos... blockPoses) {
        for (BlockPos blockPos : blockPoses) {
            if (PPUtils.distanceTo(PPUtils.closestVec3d(blockPos)) > placeRange.get()) return true;
        }

        return false;
    }

    public enum Stage {
        Preparing, Piston, Redstone, Obsidian, ZeroTick, Toggle
    }

    private boolean isAir(BlockPos blockPos) {
        return mc.world.getBlockState(blockPos).isAir();
    }
}
