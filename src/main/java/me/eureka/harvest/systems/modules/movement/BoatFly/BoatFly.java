package me.eureka.harvest.systems.modules.movement.BoatFly;

import me.eureka.harvest.events.event.MoveEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.mixininterface.IVec3d;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.utils.other.TimerUtils;
import com.google.common.eventbus.Subscribe;
import net.minecraft.util.math.Vec3d;

import java.util.List;

// BoatFly for Horizon Anarchy, some code was taken from Meteor Client
@Module.Info(name = "BoatFly", category = Module.Category.Movement)
public class BoatFly extends Module {
    public Setting<Double> horizontal = register("Horizontal", 1, 0, 150, 2);
    public Setting<Double> vertical = register("Vertical", 1, 0, 50, 2);
    public Setting<Boolean> horizon = register("Horizon", false);
    public Setting<Double> fallSpeed = register("FallSpeed", 0, 0, 1, 2);
    public Setting<String> yaw = register("Yaw", List.of("Player", "Drift", "OFF"), "OFF");
    public Setting<Boolean> stopOnUnloaded = register("StopOnUnloaded", true);

    private final TimerUtils vTimer = new TimerUtils();

    @Subscribe
    public void onMove(MoveEvent.Boat event) {
        if (!yaw.get("OFF")) {
            event.boat.setYaw(mc.player.getYaw() + (yaw.get("Drift") ? 50 : 0));
        }

        Vec3d vel = getHorizontalVelocity(horizontal.get());
        double velX = vel.getX();
        double velY = 0;
        double velZ = vel.getZ();

        if (mc.options.sprintKey.isPressed()) velY -= vertical.get() / 20;
        else velY -= (horizon.get() ? 0.7 : fallSpeed.get()) / 20;

        if (mc.options.jumpKey.isPressed()) {
            if (horizon.get()) {
                if (!vTimer.passedTicks(30)) {
                    velY += vertical.get() / 20;
                } else {
                    velY -= 0.2;
                    vTimer.reset();
                }
            } else velY += vertical.get() / 20;
        }

        if (stopOnUnloaded.get()) {
            int chunkX = (int) ((mc.player.getX() + velX) / 16);
            int chunkZ = (int) ((mc.player.getZ() + velZ) / 16);

            if (mc.world.getChunkManager().isChunkLoaded(chunkX, chunkZ)) {
                ((IVec3d) event.boat.getVelocity()).set(velX, velY, velZ);
            } else {
                ((IVec3d) event.boat.getVelocity()).set(0, velY, 0);
            }
        } else ((IVec3d) event.boat.getVelocity()).set(velX, velY, velZ);
    }

    public Vec3d getHorizontalVelocity(double bps) {
        double diagonal = 1 / Math.sqrt(2);
        float yaw = mc.player.getYaw();

        Vec3d forward = Vec3d.fromPolar(0, yaw);
        Vec3d right = Vec3d.fromPolar(0, yaw + 90);
        double velX = 0;
        double velZ = 0;

        boolean a = false;
        if (mc.player.input.pressingForward) {
            velX += forward.x / 20 * bps;
            velZ += forward.z / 20 * bps;
            a = true;
        }
        if (mc.player.input.pressingBack) {
            velX -= forward.x / 20 * bps;
            velZ -= forward.z / 20 * bps;
            a = true;
        }

        boolean b = false;
        if (mc.player.input.pressingRight) {
            velX += right.x / 20 * bps;
            velZ += right.z / 20 * bps;
            b = true;
        }
        if (mc.player.input.pressingLeft) {
            velX -= right.x / 20 * bps;
            velZ -= right.z / 20 * bps;
            b = true;
        }

        if (a && b) {
            velX *= diagonal;
            velZ *= diagonal;
        }

        return new Vec3d(velX, 0, velZ);
    }
}
