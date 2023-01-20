package me.eureka.harvest.systems.modules.combat.SelfTrap;

import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.ic.util.RotationUtil;
import me.eureka.harvest.systems.utils.advanced.InvUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;

import java.util.ArrayList;
import java.util.List;

import static me.eureka.harvest.ic.util.Wrapper.mc;

public class STUtils {
    static ArrayList<BlockPos> getTrapBlocks(Setting<String> mode) {
        PlayerEntity player = mc.player;
        ArrayList<BlockPos> positions = new ArrayList<>();
        List<Entity> getEntityBoxes;

        if (byMode(mode) == null) return positions;
        for (BlockPos blockPos : getSphere(player.getBlockPos().up((Integer) byMode(mode)[0]), 3, (Integer) byMode(mode)[1])) {
            getEntityBoxes = mc.world.getOtherEntities(null, new Box(blockPos), entity -> entity == player);
            if (!getEntityBoxes.isEmpty() || !mc.world.isAir(blockPos)) continue;

            for (Direction direction : Direction.values()) {
                if (direction == Direction.UP || ((Boolean) byMode(mode)[2]) && direction == Direction.DOWN) continue;

                getEntityBoxes = mc.world.getOtherEntities(null, new Box(blockPos.offset(direction)), entity -> entity == player);
                if (!getEntityBoxes.isEmpty()) positions.add(blockPos);
            }
        }

        return positions;
    }

    static Object[] byMode(Setting<String> mode) {
        if (mode.get("Full")) return new Object[]{2,1, false};
        if (mode.get("Head")) return new Object[]{2,1, true};
        return new Object[]{3,1, false};
    }

    static List<BlockPos> getSphere(BlockPos centerPos, int radius, int height) {
        ArrayList<BlockPos> blocks = new ArrayList<>();

        for (int i = centerPos.getX() - radius; i < centerPos.getX() + radius; i++) {
            for (int j = centerPos.getY() - height; j < centerPos.getY() + height; j++) {
                for (int k = centerPos.getZ() - radius; k < centerPos.getZ() + radius; k++) {
                    BlockPos pos = new BlockPos(i, j, k);
                    if (distanceBetween(centerPos, pos) <= radius && !blocks.contains(pos)) blocks.add(pos);
                }
            }
        }

        return blocks;
    }

    static double distanceBetween(BlockPos blockPos1, BlockPos blockPos2) {
        double d = blockPos1.getX() - blockPos2.getX();
        double e = blockPos1.getY() - blockPos2.getY();
        double f = blockPos1.getZ() - blockPos2.getZ();
        return MathHelper.sqrt((float) (d * d + e * e + f * f));
    }

    public static void place(BlockPos pos, boolean rotate,int slot, boolean clientSwing) {
        if (pos != null) {
            int prevSlot = mc.player.getInventory().selectedSlot;
            InvUtils.swap(slot);
            if (rotate) RotationUtil.rotate(pos);
            BlockHitResult result = new BlockHitResult(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), Direction.DOWN, pos, true);
            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result, 0));
            if (clientSwing) mc.player.swingHand(Hand.MAIN_HAND);
            else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            InvUtils.swap(prevSlot);
        }
    }

    public static boolean isSurrounded(){
        ArrayList<BlockPos> positions = new ArrayList<>();
        List<Entity> getEntityBoxes;

        for (BlockPos blockPos : getSphere(mc.player.getBlockPos(), 3, 1)) {
            if (!mc.world.getBlockState(blockPos).getMaterial().isReplaceable()) continue;
            getEntityBoxes = mc.world.getOtherEntities(null, new Box(blockPos), entity -> entity == mc.player);
            if (!getEntityBoxes.isEmpty()) continue;

            for (Direction direction : Direction.values()) {
                if (direction == Direction.UP || direction == Direction.DOWN) continue;

                getEntityBoxes = mc.world.getOtherEntities(null, new Box(blockPos.offset(direction)), entity -> entity == mc.player);
                if (!getEntityBoxes.isEmpty()) positions.add(blockPos);
            }
        }

        return positions.isEmpty();
    }
}
