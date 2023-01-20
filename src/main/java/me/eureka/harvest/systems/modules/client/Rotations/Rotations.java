package me.eureka.harvest.systems.modules.client.Rotations;

import com.google.common.eventbus.Subscribe;
import me.eureka.harvest.events.event.SendMovementPacketsEvent;
import me.eureka.harvest.events.event.TickEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.mixins.ChatHudMixin;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.utils.advanced.BlockPosX;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

@Module.Info(name = "Rotations", category = Module.Category.Client, drawn = false)
public class Rotations extends Module {
    private final Setting<Integer> rotationTicks = register("RotationTicks", 4, 0, 20);
    public final Setting<Boolean> thirdPerson = register("ThirdPerson", true);

    private final Pool<Rotation> rotationPool = new Pool<>(Rotation::new);
    private final List<Rotation> rotations = new ArrayList<>();
    public float serverYaw;
    public float serverPitch;
    public int rotationTimer;
    private float preYaw, prePitch;
    private int i = 0;

    private Rotation lastRotation;
    private int lastRotationTimer;
    private boolean sentLastRotation;
    public boolean rotating = false;

    public void rotate(BlockPosX bp) {
        double yaw = getYaw(bp.get());
        double pitch = getPitch(bp.get());

        rotate(yaw, pitch);
    }

    public void rotate(double yaw, double pitch, int priority, boolean clientSide, Runnable callback) {
        if (!isActive()) return;

        Rotation rotation = rotationPool.get();
        rotation.set(yaw, pitch, priority, clientSide, callback);

        int i = 0;
        for (; i < rotations.size(); i++) {
            if (priority > rotations.get(i).priority) break;
        }

        rotations.add(i, rotation);
    }

    public void rotate(double yaw, double pitch, int priority, Runnable callback) {
        rotate(yaw, pitch, priority, false, callback);
    }

    public void rotate(double yaw, double pitch, Runnable callback) {
        rotate(yaw, pitch, 0, callback);
    }

    public void rotate(double yaw, double pitch) {
        rotate(yaw, pitch, 0, null);
    }

    public void clientRotate(double yaw, double pitch) {
        mc.player.setYaw((float) yaw);
        mc.player.setPitch((float) pitch);
    }

    private void resetLastRotation() {
        if (lastRotation != null) {
            rotationPool.free(lastRotation);

            lastRotation = null;
            lastRotationTimer = 0;
        }
    }

    @Subscribe
    private void onSendMovementPacketsPre(SendMovementPacketsEvent.Pre event) {
        if (mc.cameraEntity != mc.player) return;
        sentLastRotation = false;

        if (!rotations.isEmpty()) {
            rotating = true;
            resetLastRotation();

            Rotation rotation = rotations.get(i);
            setupMovementPacketRotation(rotation);

            if (rotations.size() > 1) rotationPool.free(rotation);

            i++;
        } else if (lastRotation != null) {
            if (lastRotationTimer >= rotationTicks.get()) {
                resetLastRotation();
                rotating = false;
            } else {
                setupMovementPacketRotation(lastRotation);
                sentLastRotation = true;

                lastRotationTimer++;
            }
        }
    }

    private void setupMovementPacketRotation(Rotation rotation) {
        setClientRotation(rotation);
        setCamRotation(rotation.yaw, rotation.pitch);
    }

    private void setClientRotation(Rotation rotation) {
        preYaw = mc.player.getYaw();
        prePitch = mc.player.getPitch();

        mc.player.setYaw((float) rotation.yaw);
        mc.player.setPitch((float) rotation.pitch);
    }

    @Subscribe
    private void onSendMovementPacketsPost(SendMovementPacketsEvent.Post event) {
        if (!rotations.isEmpty()) {
            if (mc.cameraEntity == mc.player) {
                rotations.get(i - 1).runCallback();

                if (rotations.size() == 1) lastRotation = rotations.get(i - 1);

                resetPreRotation();
            }

            for (; i < rotations.size(); i++) {
                Rotation rotation = rotations.get(i);

                setCamRotation(rotation.yaw, rotation.pitch);
                if (rotation.clientSide) setClientRotation(rotation);
                rotation.sendPacket();
                if (rotation.clientSide) resetPreRotation();

                if (i == rotations.size() - 1) lastRotation = rotation;
                else rotationPool.free(rotation);
            }

            rotations.clear();
            i = 0;
        } else if (sentLastRotation) {
            resetPreRotation();
        }
    }

    private void resetPreRotation() {
        mc.player.setYaw(preYaw);
        mc.player.setPitch(prePitch);
    }

    @Subscribe
    private void onTick(TickEvent.Pre event) {
        rotationTimer++;
    }

    public double getYaw(Entity entity) {
        return mc.player.getYaw() + MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(entity.getZ() - mc.player.getZ(), entity.getX() - mc.player.getX())) - 90f - mc.player.getYaw());
    }

    public double getYaw(Vec3d pos) {
        return mc.player.getYaw() + MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(pos.getZ() - mc.player.getZ(), pos.getX() - mc.player.getX())) - 90f - mc.player.getYaw());
    }

    public double getPitch(Vec3d pos) {
        double diffX = pos.getX() - mc.player.getX();
        double diffY = pos.getY() - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double diffZ = pos.getZ() - mc.player.getZ();

        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        return mc.player.getPitch() + MathHelper.wrapDegrees((float) -Math.toDegrees(Math.atan2(diffY, diffXZ)) - mc.player.getPitch());
    }

    public double getPitch(Entity entity, Target target) {
        double y;
        if (target == Target.Head) y = entity.getEyeY();
        else if (target == Target.Body) y = entity.getY() + entity.getHeight() / 2;
        else y = entity.getY();

        double diffX = entity.getX() - mc.player.getX();
        double diffY = y - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double diffZ = entity.getZ() - mc.player.getZ();

        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        return mc.player.getPitch() + MathHelper.wrapDegrees((float) -Math.toDegrees(Math.atan2(diffY, diffXZ)) - mc.player.getPitch());
    }

    public double getPitch(Entity entity) {
        return getPitch(entity, Target.Body);
    }

    public double getYaw(BlockPos pos) {
        return mc.player.getYaw() + MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(pos.getZ() + 0.5 - mc.player.getZ(), pos.getX() + 0.5 - mc.player.getX())) - 90f - mc.player.getYaw());
    }

    public boolean canSee(PlayerEntity player, BlockPos blockPos) {
        double yaw = player.getYaw();

        double playerYaw = yaw + MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(blockPos.getZ() + 0.5 - player.getZ(), blockPos.getX() + 0.5 - player.getX())) - 90f - yaw);
        return playerYaw > yaw - 45 && playerYaw < yaw + 45;
    }

    public double getPitch(BlockPos pos) {
        double diffX = pos.getX() + 0.5 - mc.player.getX();
        double diffY = pos.getY() + 0.5 - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double diffZ = pos.getZ() + 0.5 - mc.player.getZ();

        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        return mc.player.getPitch() + MathHelper.wrapDegrees((float) -Math.toDegrees(Math.atan2(diffY, diffXZ)) - mc.player.getPitch());
    }

    public void setCamRotation(double yaw, double pitch) {
        serverYaw = (float) yaw;
        serverPitch = (float) pitch;
        rotationTimer = 0;
    }

    public enum Target {
        Head, Body, Feet
    }

    public static Rotations instance;

    public Rotations() {
        instance = this;
    }
}
