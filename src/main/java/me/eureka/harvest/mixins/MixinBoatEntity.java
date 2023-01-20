package me.eureka.harvest.mixins;

import me.eureka.harvest.Hack;
import me.eureka.harvest.events.event.MoveEvent;
import me.eureka.harvest.systems.modules.movement.BoatFly.BoatFly;
import net.minecraft.entity.vehicle.BoatEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BoatEntity.class, priority = 1050)
public class MixinBoatEntity {
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/vehicle/BoatEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V"))
    private void onTick(CallbackInfo info) {
        Hack.event().post(new MoveEvent.Boat((BoatEntity) (Object) this));
    }
}