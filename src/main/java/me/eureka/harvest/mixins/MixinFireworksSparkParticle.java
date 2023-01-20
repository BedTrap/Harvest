package me.eureka.harvest.mixins;

import me.eureka.harvest.systems.modules.render.NoRender.NoRender;
import net.minecraft.client.particle.FireworksSparkParticle;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = {FireworksSparkParticle.Flash.class})
public class MixinFireworksSparkParticle {
    @Inject(method = "buildGeometry", at = @At("HEAD"), cancellable = true)
    private void buildExplosionGeometry(VertexConsumer vertexConsumer, Camera camera, float tickDelta, CallbackInfo info) {
        if (NoRender.get.isActive() && NoRender.get.rockets.get()) info.cancel();
    }
}
