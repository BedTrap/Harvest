package me.eureka.harvest.systems.modules.world.AutoMine;

import me.eureka.harvest.Hack;
import me.eureka.harvest.ic.Friends;
import me.eureka.harvest.systems.utils.advanced.BlockPosX;
import me.eureka.harvest.ic.util.Wrapper;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tag.FluidTags;
import net.minecraft.util.math.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class PMUtils {
    public static double progress = 0;

    public static List<BlockPos> getSphere(BlockPos centerPos, double radius, double height) {
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

    public static double distanceTo(BlockPos blockPos) {
        return distanceTo(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    public static double distanceTo(Vec3d vec3d) {
        return distanceTo(vec3d.getX(), vec3d.getY(), vec3d.getZ());
    }

    public static double distanceTo(double x, double y, double z) {
        Vec3d eyePos = new Vec3d(Wrapper.mc.player.getX(), Wrapper.mc.player.getY() + Wrapper.mc.player.getEyeHeight(Wrapper.mc.player.getPose()), Wrapper.mc.player.getZ());
        Vec3d vec3d = closestVec3d(Box.from(new Vec3d(x, y, z)));

        float f = (float) (eyePos.getX() - vec3d.x);
        float g = (float) (eyePos.getY() - vec3d.y);
        float h = (float) (eyePos.getZ() - vec3d.z);
        return MathHelper.sqrt(f * f + g * g + h * h);
    }

    public static double distanceTo(BlockPos blockPos1, BlockPos blockPos2) {
        double d = blockPos1.getX() - blockPos2.getX();
        double e = blockPos1.getY() - blockPos2.getY();
        double f = blockPos1.getZ() - blockPos2.getZ();
        return MathHelper.sqrt((float) (d * d + e * e + f * f));
    }

    public static double distanceTo(BlockPos blockPos1, PlayerEntity player) {
        if (blockPos1 == null || player == null) return 99;

        double d = blockPos1.getX() - player.getX();
        double e = blockPos1.getY() - player.getY();
        double f = blockPos1.getZ() - player.getZ();
        return MathHelper.sqrt((float) (d * d + e * e + f * f));
    }

    public static boolean isPlayerNear(BlockPosX blockPos) {
        if (blockPos == null) return false;

        for (PlayerEntity player : Wrapper.mc.world.getPlayers()) {
            if (player == null) continue;
            if (distanceTo(blockPos, player) > 5) continue;

            if (player.isDead()) continue;
            if (player == Wrapper.mc.player) continue;
            if (Hack.friends().isFriend(player)) continue;

            if (getBlocksAround(player).contains(blockPos)) return true;
        }

        return false;
    }

    public static List<BlockPosX> getBlocksAround(PlayerEntity player) {
        List<BlockPosX> positions = new ArrayList<>();
        List<Entity> getEntityBoxes;

        for (BlockPos blockPos : getSphere(player.getBlockPos(), 3, 1)) {
            getEntityBoxes = Wrapper.mc.world.getOtherEntities(null, new Box(blockPos), entity -> entity == player);
            if (!getEntityBoxes.isEmpty()) continue;

            for (Direction direction : Direction.values()) {
                if (direction == Direction.UP || direction == Direction.DOWN) continue;

                getEntityBoxes = Wrapper.mc.world.getOtherEntities(null, new Box(blockPos.offset(direction)), entity -> entity == player);
                if (!getEntityBoxes.isEmpty()) positions.add(new BlockPosX(blockPos));
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

    public static EndCrystalEntity getEntity(BlockPos blockPos) {
        if (blockPos == null) return null;

        return hasEntity(new Box(blockPos), entity -> entity instanceof EndCrystalEntity) ?
                (EndCrystalEntity) Wrapper.mc.world.getOtherEntities(null, new Box(blockPos), entity -> entity instanceof EndCrystalEntity).get(0) : null;
    }

    public static Vec3d closestVec3d(Box box) {
        if (box == null) return new Vec3d(0.0, 0.0, 0.0);
        Vec3d eyePos = new Vec3d(Wrapper.mc.player.getX(), Wrapper.mc.player.getY() + Wrapper.mc.player.getEyeHeight(Wrapper.mc.player.getPose()), Wrapper.mc.player.getZ());

        double x = MathHelper.clamp(eyePos.getX(), box.minX, box.maxX);
        double y = MathHelper.clamp(eyePos.getY(), box.minY, box.maxY);
        double z = MathHelper.clamp(eyePos.getZ(), box.minZ, box.maxZ);

        return new Vec3d(x, y, z);
    }

    // Progress
    public static boolean canBreak(int slot, BlockPos blockPos) {
        if (progress >= 1) return true;
        BlockState blockState = Wrapper.mc.world.getBlockState(blockPos);

        if (progress < 1)
            progress += getBreakDelta(slot != 420 ? slot : Wrapper.mc.player.getInventory().selectedSlot, blockState);
        return false;
    }

    public static double getBreakDelta(int slot, BlockState state) {
        float hardness = state.getHardness(null, null);

        if (hardness == -1) return 0;
        else {
            return getBlockBreakingSpeed(slot, state) / hardness / (!state.isToolRequired() || Wrapper.mc.player.getInventory().main.get(slot).isSuitableFor(state) ? 30 : 100);
        }
    }

    private static double getBlockBreakingSpeed(int slot, BlockState block) {
        double speed = Wrapper.mc.player.getInventory().main.get(slot).getMiningSpeedMultiplier(block);

        if (speed > 1) {
            ItemStack tool = Wrapper.mc.player.getInventory().getStack(slot);

            int efficiency = EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, tool);

            if (efficiency > 0 && !tool.isEmpty()) speed += efficiency * efficiency + 1;
        }

        if (StatusEffectUtil.hasHaste(Wrapper.mc.player)) {
            speed *= 1 + (StatusEffectUtil.getHasteAmplifier(Wrapper.mc.player) + 1) * 0.2F;
        }

        if (Wrapper.mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            float k = switch (Wrapper.mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) {
                case 0 -> 0.3F;
                case 1 -> 0.09F;
                case 2 -> 0.0027F;
                default -> 8.1E-4F;
            };

            speed *= k;
        }

        if (Wrapper.mc.player.isSubmergedIn(FluidTags.WATER) && !EnchantmentHelper.hasAquaAffinity(Wrapper.mc.player)) {
            speed /= 5.0F;
        }

        if (!Wrapper.mc.player.isOnGround()) {
            speed /= 5.0F;
        }

        return speed;
    }
}