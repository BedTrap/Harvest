package me.eureka.harvest.systems.modules.render.ViewModel;

import me.eureka.harvest.events.event.HeldItemRendererEvent;
import me.eureka.harvest.events.event.PacketEvent;
import me.eureka.harvest.events.event.TickEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.systems.modules.Module;
import com.google.common.eventbus.Subscribe;
import me.eureka.harvest.systems.utils.other.TimerUtils;
import net.minecraft.item.PotionItem;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3f;

import java.util.List;

@Module.Info(name = "ViewModel", category = Module.Category.Render)
public class ViewModel extends Module {
    // Scale
    public Setting<Double> scaleMainX = register("ScaleMainX", 1, 0, 2, 2);
    public Setting<Double> scaleMainY = register("ScaleMainY", 1, 0, 2, 2);
    public Setting<Double> scaleMainZ = register("ScaleMainZ", 1, 0, 2, 2);
    public Setting<Double> scaleOffX = register("ScaleOffX", 1, 0, 2, 2);
    public Setting<Double> scaleOffY = register("ScaleOffY", 1, 0, 2, 2);
    public Setting<Double> scaleOffZ = register("ScaleOffZ", 1, 0, 2, 2);
    public Setting<Boolean> scaleReset = register("ScaleReset", false);

    // Position
    public Setting<Double> posMainX = register("PosMainX", 1, -2, 2, 2);
    public Setting<Double> posMainY = register("PosMainY", -0.4, -2, 2, 2);
    public Setting<Double> posMainZ = register("PosMainZ", -1, -2, 2, 2);
    public Setting<Double> posOffX = register("PosOffX", 1, -2, 2, 2);
    public Setting<Double> posOffY = register("PosOffY", -0.4, -2, 2, 2);
    public Setting<Double> posOffZ = register("PosOffZ", -1, -2, 2, 2);
    public Setting<Boolean> posReset = register("PosReset", false);

    // Rotation
    public Setting<Integer> rotMainX = register("RotMainX", 0, -180, 180);
    public Setting<Integer> rotMainY = register("RotMainY", 0, -180, 180);
    public Setting<Integer> rotMainZ = register("RotMainZ", 0, -180, 180);
    public Setting<Integer> rotOffX = register("RotOffX", 0, -180, 180);
    public Setting<Integer> rotOffY = register("RotOffY", 0, -180, 180);
    public Setting<Integer> rotOffZ = register("RotOffZ", 0, -180, 180);
    public Setting<Boolean> rotReset = register("RotReset", false);

    // Eat
    public Setting<Double> eatMainX = register("EatMainX", 0, -2, 2, 2);
    public Setting<Double> eatMainZ = register("EatMainZ", 0, -2, 2, 2);
    public Setting<Double> eatOffX = register("EatOffX", 0, -2, 2, 2);
    public Setting<Double> eatOffZ = register("EatOffZ", 0, -2, 2, 2);
    public Setting<Boolean> eatReset = register("EatReset", false);

    @Subscribe
    public void onHand(HeldItemRendererEvent.Hand event) {
        if (!isActive()) return;

        switch (event.hand) {
            case MAIN_HAND -> {
                event.matrix.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(rotMainX.get()));
                event.matrix.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(rotMainY.get()));
                event.matrix.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(rotMainZ.get()));
                event.matrix.scale(scaleMainX.get().floatValue(), scaleMainY.get().floatValue(), scaleMainZ.get().floatValue());
                if (isUsingItem(event.hand)) {
                    event.matrix.translate(posMainX.get() - posMainX.get() + eatMainX.get(), posMainY.get() -0.6D, posMainZ.get() - posMainZ.get() + eatMainZ.get());
                } else {
                    event.matrix.translate(posMainX.get(), posMainY.get(), posMainZ.get());
                }
            }
            case OFF_HAND -> {
                event.matrix.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(rotOffX.get()));
                event.matrix.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(rotOffY.get()));
                event.matrix.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(rotOffZ.get()));
                event.matrix.scale(scaleOffX.get().floatValue(), scaleOffY.get().floatValue(), scaleOffZ.get().floatValue());
                if (isUsingItem(event.hand)) {
                    event.matrix.translate(-posOffX.get() - posOffX.get() + eatOffX.get(), posOffY.get() -0.6D, posOffZ.get() - posOffZ.get() + eatOffZ.get());
                } else {
                    event.matrix.translate(-posOffX.get(), posOffY.get(), posOffZ.get());
                }
            }
        }

        if (scaleReset.get()) {
            scaleMainX.setValue(1.0);
            scaleMainY.setValue(1.0);
            scaleMainZ.setValue(1.0);
            scaleOffX.setValue(1.0);
            scaleOffY.setValue(1.0);
            scaleOffZ.setValue(1.0);
            scaleReset.setValue(false);
        }

        if (posReset.get()) {
            posMainX.setValue(1.0);
            posMainY.setValue(-0.4);
            posMainZ.setValue(-1.0);
            posOffX.setValue(1.0);
            posOffY.setValue(-0.4);
            posOffZ.setValue(-1.0);
            posReset.setValue(false);
        }

        if (rotReset.get()) {
            rotMainX.setValue(0);
            rotMainY.setValue(0);
            rotMainZ.setValue(0);
            rotOffX.setValue(0);
            rotOffX.setValue(0);
            rotOffX.setValue(0);
            rotReset.setValue(false);
        }

        if (eatReset.get()) {
            eatMainX.setValue(0.0);
            eatMainZ.setValue(0.0);
            eatOffX.setValue(0.0);
            eatOffZ.setValue(0.0);
            eatReset.setValue(false);
        }
    }

    private boolean isUsingItem(Hand hand) {
        return switch (hand) {
            case MAIN_HAND -> mc.player.isUsingItem() && (mc.player.getMainHandStack().getItem() instanceof PotionItem || mc.player.getMainHandStack().getItem().isFood());
            case OFF_HAND -> mc.player.isUsingItem() && (mc.player.getOffHandStack().getItem() instanceof PotionItem || mc.player.getOffHandStack().getItem().isFood());
        };
    }

    public static ViewModel get;

    public ViewModel() {
        get = this;
    }
}