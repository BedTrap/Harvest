package me.eureka.harvest.systems.modules.combat.FeetTrap;

import me.eureka.harvest.ic.util.RotationUtil;
import me.eureka.harvest.systems.utils.advanced.FindItemResult;
import me.eureka.harvest.systems.utils.advanced.InvUtils;
import me.eureka.harvest.ic.util.Wrapper;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static me.eureka.harvest.systems.modules.Module.mc;

public class FTUtils {
    static List<BlockPos> getSphere(BlockPos centerPos, double radius, double height) {
        ArrayList<BlockPos> blocks = new ArrayList<>();

        for (int i = centerPos.getX() - (int) radius; i < centerPos.getX() + radius; i++) {
            for (int j = centerPos.getY() - (int) height; j < centerPos.getY() + height; j++) {
                for (int k = centerPos.getZ() - (int) radius; k < centerPos.getZ() + radius; k++) {
                    BlockPos pos = new BlockPos(i, j, k);

                    if (distanceTo(centerPos, pos) <= radius && !blocks.contains(pos)) blocks.add(pos);
                }
            }
        }

        return blocks;
    }

    static double distanceTo(BlockPos blockPos1, BlockPos blockPos2) {
        double d = blockPos1.getX() - blockPos2.getX();
        double e = blockPos1.getY() - blockPos2.getY();
        double f = blockPos1.getZ() - blockPos2.getZ();
        return MathHelper.sqrt((float) (d * d + e * e + f * f));
    }

    public static void place(BlockPos pos, boolean rotate, int slot) {
        if (pos != null) {
            int prevSlot = mc.player.getInventory().selectedSlot;
            InvUtils.swap(slot);
            if (rotate) RotationUtil.rotate(pos);
            BlockHitResult result = new BlockHitResult(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), Direction.DOWN, pos, true);
            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result, 0));
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            InvUtils.swap(prevSlot);
        }
    }

    public static FindItemResult getBlock() {
        return InvUtils.findInHotbar(itemStack -> Block.getBlockFromItem(itemStack.getItem()).getBlastResistance() >= 600);
    }

    public static boolean isSurrounded(PlayerEntity p) {
        ArrayList<BlockPos> positions = new ArrayList<>();
        List<Entity> getEntityBoxes;

        for (BlockPos blockPos : getSphere(p.getBlockPos(), 3, 1)) {
            if (!Wrapper.mc.world.getBlockState(blockPos).getMaterial().isReplaceable()) continue;
            getEntityBoxes = Wrapper.mc.world.getOtherEntities(null, new Box(blockPos), entity -> entity == p);
            if (!getEntityBoxes.isEmpty()) continue;

            for (Direction direction : Direction.values()) {
                if (direction == Direction.UP || direction == Direction.DOWN) continue;

                getEntityBoxes = Wrapper.mc.world.getOtherEntities(null, new Box(blockPos.offset(direction)), entity -> entity == p);
                if (!getEntityBoxes.isEmpty()) positions.add(blockPos);
            }
        }

        return positions.isEmpty();
    }

    static ArrayList<BlockPos> getSurroundBlocks(PlayerEntity player) {
        ArrayList<BlockPos> positions = new ArrayList<>();
        List<Entity> getEntityBoxes;

        for (BlockPos blockPos : getSphere(player.getBlockPos(), 3, 1)) {
            getEntityBoxes = Wrapper.mc.world.getOtherEntities(null, new Box(blockPos), entity -> entity == player);
            if (!getEntityBoxes.isEmpty()) continue;

            for (Direction direction : Direction.values()) {
                if (direction == Direction.UP || direction == Direction.DOWN) continue;

                getEntityBoxes = Wrapper.mc.world.getOtherEntities(null, new Box(blockPos.offset(direction)), entity -> entity == player);
                if (!getEntityBoxes.isEmpty()) positions.add(blockPos);
            }
        }

        return positions;
    }

    public static boolean hasEntity(Box box) {
        return hasEntity(box, entity -> entity instanceof PlayerEntity || entity instanceof EndCrystalEntity || entity instanceof TntEntity);
    }

    public static boolean hasEntity(Box box, Predicate<Entity> predicate) {
        return !Wrapper.mc.world.getOtherEntities(null, box, predicate).isEmpty();
    }
}
