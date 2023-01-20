package me.eureka.harvest.systems.modules.movement.HighwayFly;

import com.google.common.eventbus.Subscribe;
import me.eureka.harvest.events.event.EventBlockShape;
import me.eureka.harvest.events.event.TickEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.systems.modules.Module;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShapes;

// This is my first real working ElytraFly (I called it HighwayFly because it only on highways or in the sky)
// Works on karasique.com (nag1bator228 don't kill me please)
@Module.Info(name = "HighwayFly", category = Module.Category.Movement)
public class HighwayFly extends Module {
    public Setting<Double> speed = register("Speed", 0.39, 0.1, 0.50, 2);
    public Setting<Double> pitch = register("Pitch", 5.30, -10.0, 0.0, 2); // See MixinLivingEntity.java
    public Setting<Boolean> lockYaw = register("LockYaw", false);
    public Setting<Boolean> roof = register("Roof", true);

    private int y;

    @Override
    public void onActivate() {
        y = mc.player.getBlockY() + 3;
    }

    @Subscribe
    public void onShape(EventBlockShape event) {
        if (!roof.get()) return;
        if (event.bp.getY() >= y) event.shape = VoxelShapes.fullCube();
    }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        mc.player.setPitch(pitch.get().floatValue());

        if (lockYaw.get()) {
            mc.player.setYaw(mc.player.getHorizontalFacing().asRotation());
        }

        float yaw = (float) Math.toRadians(mc.player.getYaw());
        if (mc.options.forwardKey.isPressed()) {
            mc.player.addVelocity(-MathHelper.sin(yaw) * speed.get() / 20, 0, MathHelper.cos(yaw) * speed.get() / 20);
        }
    }

    public float getPitch() {
        if (!isActive()) return mc.player.getPitch();

        if (mc.options.forwardKey.isPressed()) {
            return pitch.get().floatValue();
        } else return mc.player.getPitch();
    }

    public boolean shouldRotate() {
        if (!isActive()) return false;
        if (mc.player.isOnGround()) return false;

        return mc.options.forwardKey.isPressed();
    }

    public static HighwayFly INSTANCE;

    public HighwayFly() {
        INSTANCE = this;
    }

    {
        pitch.setDescription("Doesn't work on strict servers, be careful with this.");
    }
}