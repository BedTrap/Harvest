package me.eureka.harvest.mixins;

import me.eureka.harvest.systems.modules.movement.HighwayFly.HighwayFly;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer<T extends LivingEntity, M extends EntityModel<T>> {
    //3rd Person Rotation

//    @ModifyVariable(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", ordinal = 2, at = @At(value = "STORE", ordinal = 0))
//    public float changeYaw(float oldValue, LivingEntity entity) {
//        if (entity.equals(mc.player) && Rotations.rotationTimer < 10) return Rotations.serverYaw;
//        return oldValue;
//    }
//
//    @ModifyVariable(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", ordinal = 3, at = @At(value = "STORE", ordinal = 0))
//    public float changeHeadYaw(float oldValue, LivingEntity entity) {
//        if (entity.equals(mc.player) && Rotations.rotationTimer < 10) return Rotations.serverYaw;
//        return oldValue;
//    }

    @ModifyVariable(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", ordinal = 5, at = @At(value = "STORE", ordinal = 3))
    public float changePitch(float oldValue, LivingEntity entity) {
        return HighwayFly.INSTANCE.shouldRotate() ? HighwayFly.INSTANCE.getPitch() : oldValue;
    }
}