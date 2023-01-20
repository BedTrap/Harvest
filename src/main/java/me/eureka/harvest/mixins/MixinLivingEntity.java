package me.eureka.harvest.mixins;

import me.eureka.harvest.Hack;
import me.eureka.harvest.events.event.MoveEvent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static me.eureka.harvest.ic.util.Wrapper.mc;

@Mixin(value = LivingEntity.class, priority = 1050)
public class MixinLivingEntity {
//    @ModifyArg(method = "swingHand(Lnet/minecraft/util/Hand;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;swingHand(Lnet/minecraft/util/Hand;Z)V"))
//    private Hand setHand(Hand hand) {
////        if (Swing.INSTANCE.isActive()) {
////            if (Swing.INSTANCE.mode.get("None")) return hand;
////            return Swing.INSTANCE.mode.get("OffHand") ? Hand.OFF_HAND : Hand.MAIN_HAND;
////        }
//
//        return hand;
//    }

    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V", ordinal = 2))
    public void elytraMove(LivingEntity livingEntity, MovementType movementType, Vec3d vec3d) {
        if (mc.player == null || mc.world == null) return;

        MoveEvent.Elytra event = new MoveEvent.Elytra(vec3d);
        Hack.event().post(event);

        livingEntity.move(movementType, vec3d);
    }

//    @Redirect(method = "travel(Lnet/minecraft/util/math/Vec3d;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getPitch()F"))
//    public float getPitch(LivingEntity instance) {
//        return HighwayFly.INSTANCE.getPitch();
//    }

//    @ModifyConstant(method = "getHandSwingDuration", constant = @Constant(intValue = 6))
//    private int getHandSwingDuration(int constant) {
//        if ((Object) this != mc.player) return constant;
//        return ViewModel.get.isActive() && ViewModel.get.packet.get() && mc.options.getPerspective().isFirstPerson() ? 1 : constant;
//    }
}
