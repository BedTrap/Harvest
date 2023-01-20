package me.eureka.harvest.systems.modules.movement.LongJump;

import me.eureka.harvest.events.event.TickEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.systems.modules.Module;
import com.google.common.eventbus.Subscribe;
import net.minecraft.util.math.MathHelper;


@Module.Info(name = "LongJump", category = Module.Category.Movement)
public class LongJump extends Module {
    public Setting<Double> speed = register("Factor", 1, 0.1, 3, 2);

    private boolean jump;

    @Override
    public void onActivate() {
        jump = false;
    }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        if (mc.options.jumpKey.isPressed()) jump = true;

        if (!jump) return;
        if (mc.player.isOnGround()) return;
        float yaw = (float) Math.toRadians(mc.player.getYaw());
        double vSpeed = speed.get() / 5;

        if (!mc.player.isOnGround()) {
            mc.player.addVelocity(-MathHelper.sin(yaw) * vSpeed, 0.0F, MathHelper.cos(yaw) * vSpeed);
        } else if (mc.player.isOnGround()) {
            mc.player.setVelocity(0, 0, 0);
        }
        toggle();
    }

    public boolean shouldPause() {
        if (!isActive() && jump && mc.player.isOnGround()) jump = false;
        return jump;
    }

    public static LongJump INSTANCE;

    public LongJump() {
        INSTANCE = this;
    }
}