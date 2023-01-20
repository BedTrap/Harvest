package me.eureka.harvest.ic.util;

import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import static me.eureka.harvest.ic.util.Wrapper.mc;

public class RotationUtil {

    public static float startYaw, startPitch;

    private static final Vec3d eyesPos = new Vec3d(mc.player.getX(),
            mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()),
            mc.player.getZ());

    public static void rotate(BlockPos blockPos) {
        startYaw = mc.player.getYaw();
        startPitch = mc.player.getPitch();
        Vec3d vec = new Vec3d(blockPos.getX(), blockPos.getY(), blockPos.getZ());
        double diffX = vec.x - eyesPos.x;
        double diffY = vec.y - eyesPos.y;
        double diffZ = vec.z - eyesPos.z;

        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90F;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, diffXZ));

        float[] rotations = {
                mc.player.getYaw()
                        + MathHelper.wrapDegrees(yaw - mc.player.getYaw()),
                mc.player.getPitch() + MathHelper
                        .wrapDegrees(pitch - mc.player.getPitch())};

        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(rotations[0], rotations[1], mc.player.isOnGround()));
    }

    public static void rotate(Entity entity, boolean clientSide) {
        startYaw = mc.player.getYaw();
        startPitch = mc.player.getPitch();
        Vec3d vec = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
        double diffX = vec.x - eyesPos.x;
        double diffY = vec.y - eyesPos.y;
        double diffZ = vec.z - eyesPos.z;

        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90F;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, diffXZ));

        float[] rotations = {
                mc.player.getYaw()
                        + MathHelper.wrapDegrees(yaw - mc.player.getYaw()),
                mc.player.getPitch() + MathHelper
                        .wrapDegrees(pitch - mc.player.getPitch())};

        if (clientSide) {
            mc.player.setYaw(rotations[0]);
            mc.player.setPitch(rotations[1]);
        } else
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(rotations[0], rotations[1], mc.player.isOnGround()));
    }

    public static void rotate(float yaw, float pitch) {
        startYaw = mc.player.getYaw();
        startPitch = mc.player.getPitch();
        float[] rotations = {
                mc.player.getYaw()
                        + MathHelper.wrapDegrees(yaw - mc.player.getYaw()),
                mc.player.getPitch() + MathHelper
                        .wrapDegrees(pitch - mc.player.getPitch())};

        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(rotations[0], rotations[1], mc.player.isOnGround()));
    }

    public static void rotate(float yaw, float pitch, Runnable callback) {
        rotate(yaw, pitch);
        callback.run();
    }
}
