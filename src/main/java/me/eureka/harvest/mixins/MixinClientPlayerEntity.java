package me.eureka.harvest.mixins;

import me.eureka.harvest.Hack;
import me.eureka.harvest.events.event.MoveEvent;
import me.eureka.harvest.events.event.SendMovementPacketsEvent;
import me.eureka.harvest.events.event.SprintEvent;
import me.eureka.harvest.systems.modules.movement.NoSlow.NoSlow;
import me.eureka.harvest.systems.modules.movement.Velocity.Velocity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class MixinClientPlayerEntity {
    @Inject(method = "move", at = @At("HEAD"))
    public void move(MovementType holeType, Vec3d movement, CallbackInfo ci) {
        MoveEvent.Entity event = new MoveEvent.Entity(movement);
        Hack.event().post(event);
    }

    @Inject(method = "pushOutOfBlocks", at = @At("HEAD"), cancellable = true)
    private void onPushOutOfBlocks(double x, double d, CallbackInfo info) {
        if (Velocity.get.isActive() && Velocity.get.push.get()) {
            info.cancel();
        }
    }

    @Inject(method = "setSprinting", at = @At("HEAD"), cancellable = true)
    public void onSetSprint(boolean sprinting, CallbackInfo ci) {
        SprintEvent event = new SprintEvent(sprinting);
        Hack.event().post(event);

        if(event.isCancelled()) {
            event.setSprinting(event.isSprinting());
            ci.cancel();
        }
    }

    @Inject(method = "sendMovementPackets", at = @At("HEAD"))
    private void onSendMovementPacketsHead(CallbackInfo info) {
        SendMovementPacketsEvent.Pre event = new SendMovementPacketsEvent.Pre();
        Hack.event().post(event);
    }

    @Inject(method = "sendMovementPackets", at = @At("TAIL"))
    private void onSendMovementPacketsTail(CallbackInfo info) {
        SendMovementPacketsEvent.Post event = new SendMovementPacketsEvent.Post();
        Hack.event().post(event);
    }
}
