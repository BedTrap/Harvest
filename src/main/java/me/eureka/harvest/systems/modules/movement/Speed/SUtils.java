package me.eureka.harvest.systems.modules.movement.Speed;

import me.eureka.harvest.ic.util.Wrapper;
import net.minecraft.block.Block;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec2f;

public class SUtils {
    public static Vec2f getSpeed(double speed) {
        float forward = Wrapper.mc.player.input.movementForward;
        float side = Wrapper.mc.player.input.movementSideways;
        float yaw = Wrapper.mc.player.prevYaw + (Wrapper.mc.player.getYaw() - Wrapper.mc.player.prevYaw) * Wrapper.mc.getTickDelta();
        if (forward == 0.0f && side == 0.0f) {
            return new Vec2f(0, 0);
        }
        if (forward != 0.0f) {
            if (side >= 1.0f) {
                yaw += (float) (forward > 0.0f ? -45 : 45);
                side = 0.0f;
            } else if (side <= -1.0f) {
                yaw += (float) (forward > 0.0f ? 45 : -45);
                side = 0.0f;
            }
            if (forward > 0.0f) {
                forward = 1.0f;
            } else if (forward < 0.0f) {
                forward = -1.0f;
            }
        }
        double mx = Math.cos(Math.toRadians(yaw + 90.0f));
        double mz = Math.sin(Math.toRadians(yaw + 90.0f));
        double velX = (double) forward * speed * mx + (double) side * speed * mz;
        double velZ = (double) forward * speed * mz - (double) side * speed * mx;
        return new Vec2f((float) velX, (float) velZ);
    }

    public static double getDefaultSpeed() {
        int amplifier;
        double defaultSpeed = 0.2873;
        if (Wrapper.mc.player.hasStatusEffect(StatusEffects.SPEED)) {
            amplifier = Wrapper.mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier();
            defaultSpeed *= 1.0 + 0.2 * (double) (amplifier + 1);
        }
        if (Wrapper.mc.player.hasStatusEffect(StatusEffects.SLOWNESS)) {
            amplifier = Wrapper.mc.player.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier();
            defaultSpeed /= 1.0 + 0.2 * (double) (amplifier + 1);
        }
        return defaultSpeed;
    }

    public static double getPlayerHeight(double height) {
        StatusEffectInstance jumpBoost;
        jumpBoost = Wrapper.mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST) ? Wrapper.mc.player.getStatusEffect(StatusEffects.JUMP_BOOST) : null;
        if (jumpBoost != null) {
            height += ((float) jumpBoost.getAmplifier() + 0.1f);
        }
        return height;
    }

    public static boolean isMoving() {
        return Wrapper.mc.player.input.movementSideways != 0 || Wrapper.mc.player.input.movementForward != 0;
    }

    public static boolean doesBoxTouchBlock(Box box, Block block) {
        for (int x = (int) Math.floor(box.minX); x < Math.ceil(box.maxX); x++) {
            for (int y = (int) Math.floor(box.minY); y < Math.ceil(box.maxY); y++) {
                for (int z = (int) Math.floor(box.minZ); z < Math.ceil(box.maxZ); z++) {
                    if (Wrapper.mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() == block) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
