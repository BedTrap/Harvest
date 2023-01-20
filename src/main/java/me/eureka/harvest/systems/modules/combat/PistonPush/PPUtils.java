package me.eureka.harvest.systems.modules.combat.PistonPush;

import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.function.Predicate;

import static me.eureka.harvest.ic.util.Wrapper.mc;

public class PPUtils {
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
        return hasEntity(box, entity -> entity instanceof PlayerEntity || entity instanceof EndCrystalEntity || entity instanceof TntEntity);
    }

    public static boolean hasEntity(Box box, Predicate<Entity> predicate) {
        return !mc.world.getOtherEntities(null, box, predicate).isEmpty();
    }

    public static Vec3d closestVec3d(BlockPos blockpos) {
        if (blockpos == null) return new Vec3d(0.0, 0.0, 0.0);
        double x = MathHelper.clamp((mc.player.getX() - blockpos.getX()), 0.0, 1.0);
        double y = MathHelper.clamp((mc.player.getY() - blockpos.getY()), 0.0, 0.6);
        double z = MathHelper.clamp((mc.player.getZ() - blockpos.getZ()), 0.0, 1.0);
        return new Vec3d(blockpos.getX() + x, blockpos.getY() + y, blockpos.getZ() + z);
    }
}
