package me.eureka.harvest.mixins;

import me.eureka.harvest.systems.modules.world.Timer.Timer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderTickCounter.class)
public class MixinRenderTickCounter {

    @Shadow private float lastFrameDuration;
    @Shadow private float tickDelta;
    @Shadow private long prevTimeMillis;
    @Shadow private float tickTime;

    @Inject(method = "beginRenderTick", at = @At("HEAD"), cancellable = true)
    private void beginRenderTick(long timeMillis, CallbackInfoReturnable<Integer> ci) {
            this.lastFrameDuration = (float) (((timeMillis - this.prevTimeMillis) / this.tickTime) * Timer.get.getMultiplier());
            this.prevTimeMillis = timeMillis;
            this.tickDelta += this.lastFrameDuration;
            int t = (int) this.tickDelta;
            this.tickDelta -= t;
            ci.setReturnValue(t);
    }
}