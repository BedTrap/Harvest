package me.eureka.harvest.systems.modules.combat.PistonCrystal;

import me.eureka.harvest.mixins.MinecraftClientAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static me.eureka.harvest.ic.util.Wrapper.mc;

public class PCUtils {
    public static List<BlockPos> getSphere(PlayerEntity target, double radius, double height, boolean considerTop, boolean ignoreAir) {
        ArrayList<BlockPos> blocks = new ArrayList<>();
        BlockPos centerPos = target.getBlockPos();

        for (int i = centerPos.getX() - (int) radius; i < centerPos.getX() + radius; i++) {
            for (int j = centerPos.getY() - (int) height; j < (considerTop ? centerPos.getY() + height : centerPos.getY()); j++) {
                for (int k = centerPos.getZ() - (int) radius; k < centerPos.getZ() + radius; k++) {
                    BlockPos pos = new BlockPos(i, j, k);

                    if (ignoreAir && mc.world.isAir(pos)) continue;
                    if (distanceBetween(centerPos, pos) <= radius && !blocks.contains(pos)) blocks.add(pos);
                }
            }
        }

        return blocks;
    }

    public static double distanceBetween(BlockPos blockPos1, BlockPos blockPos2) {
        double d = blockPos1.getX() - blockPos2.getX();
        double e = blockPos1.getY() - blockPos2.getY();
        double f = blockPos1.getZ() - blockPos2.getZ();
        return MathHelper.sqrt((float) (d * d + e * e + f * f));
    }

    public static double distanceTo(Vec3d vec3d) {
        return distanceTo(vec3d.getX(), vec3d.getY(), vec3d.getZ());
    }

    public static double distanceTo(BlockPos blockPos) {
        return distanceTo(mc.player.getBlockPos(), blockPos);
    }

    public static double distanceTo(BlockPos blockPos1, BlockPos blockPos2) {
        double d = blockPos1.getX() - blockPos2.getX();
        double e = blockPos1.getY() - blockPos2.getY();
        double f = blockPos1.getZ() - blockPos2.getZ();
        return MathHelper.sqrt((float) (d * d + e * e + f * f));
    }

    public static double distanceTo(double x, double y, double z) {
        Vec3d eyePos = new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());

        float f = (float) (eyePos.getX() - x);
        float g = (float) (eyePos.getY() - y);
        float h = (float) (eyePos.getZ() - z);
        return MathHelper.sqrt(f * f + g * g + h * h);
    }

    public static boolean hasEntity(Box box) {
        return hasEntity(box, entity -> entity instanceof PlayerEntity || entity instanceof EndCrystalEntity || entity instanceof TntEntity || entity instanceof ItemEntity);
    }

    public static boolean hasEntity(Box box, Predicate<Entity> predicate) {
        return !mc.world.getOtherEntities(null, box, predicate).isEmpty();
    }

    public static Vec3d roundVec(Entity entity) {
        BlockPos blockPos = entity.getBlockPos().down();

        return new Vec3d(blockPos.getX() + 0.5, blockPos.getY() + 1, blockPos.getZ() + 0.5);
    }

    public static void leftClick() {
        mc.options.attackKey.setPressed(true);
        ((MinecraftClientAccessor) mc).leftClick();
        mc.options.attackKey.setPressed(false);
    }

    public static Vec3d closestVec3d(BlockPos blockpos) {
        if (blockpos == null) return new Vec3d(0.0, 0.0, 0.0);
        double x = MathHelper.clamp((mc.player.getX() - blockpos.getX()), 0.0, 1.0);
        double y = MathHelper.clamp((mc.player.getY() - blockpos.getY()), 0.0, 1.0);
        double z = MathHelper.clamp((mc.player.getZ() - blockpos.getZ()), 0.0, 1.0);
        return new Vec3d(blockpos.getX() + x, blockpos.getY() + y, blockpos.getZ() + z);
    }

    public static Vec3d closestVec3d(Entity entity) {
        if (entity == null) return new Vec3d(0.0, 0.0, 0.0);

        Box box = entity.getBoundingBox();
        double x = MathHelper.clamp(mc.player.getX(), box.minX, box.maxX);
        double y = MathHelper.clamp(mc.player.getY(), box.minY, box.maxY);
        double z = MathHelper.clamp(mc.player.getZ(), box.minZ, box.maxZ);

        return new Vec3d(x, y, z);
    }

    public static int getYaw(Direction direction) {
        if (direction == null) return (int) mc.player.getYaw(1.0F);

        return switch (direction) {
            case NORTH -> 180;
            case SOUTH -> 0;
            case WEST -> 90;
            case EAST -> -90;
            default -> (int) mc.player.getYaw(1.0F);
        };
    }
}