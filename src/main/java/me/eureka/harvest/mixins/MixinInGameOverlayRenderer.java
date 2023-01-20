package me.eureka.harvest.mixins;

import me.eureka.harvest.Hack;
import me.eureka.harvest.systems.modules.render.NoRender.NoRender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameOverlayRenderer.class)
public class MixinInGameOverlayRenderer {
    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true)
    private static void onRenderFireOverlay(MinecraftClient minecraftClient, MatrixStack matrixStack, CallbackInfo ci) {
        if (Hack.modules().get("NoRender").isActive() && NoRender.get.noFireOverlay.get()) ci.cancel();
    }

    @Inject(method = "renderUnderwaterOverlay", at = @At("HEAD"), cancellable = true)
    private static void onRenderUnderwaterOverlay(MinecraftClient minecraftClient, MatrixStack matrixStack, CallbackInfo ci) {
        if (Hack.modules().get("NoRender").isActive() && NoRender.get.noUnderWaterOverlay.get()) ci.cancel();
    }

    @Inject(method = "renderInWallOverlay", at = @At("HEAD"), cancellable = true)
    private static void render(Sprite sprite, MatrixStack matrices, CallbackInfo ci) {
        if (Hack.modules().get("NoRender").isActive() && NoRender.get.noInBlockOverlay.get()) ci.cancel();
    }

}

