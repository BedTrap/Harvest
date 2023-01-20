package me.eureka.harvest.mixins;

import me.eureka.harvest.Hack;
import me.eureka.harvest.events.event.Render3DEvent;
import me.eureka.harvest.ic.util.Renderer3D;
import me.eureka.harvest.mixininterface.IVec3d;
import me.eureka.harvest.systems.modules.player.NoMiningTrace.NoMiningTrace;
import me.eureka.harvest.systems.modules.render.NoRender.NoRender;
import com.mojang.blaze3d.systems.RenderSystem;
import me.eureka.harvest.ic.util.Wrapper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {
    @Shadow
    @Final
    private Camera camera;
    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    public abstract void loadProjectionMatrix(Matrix4f matrix4f);

    @Shadow
    protected abstract void bobView(MatrixStack matrixStack, float f);

    @Shadow
    protected abstract void bobViewWhenHurt(MatrixStack matrixStack, float f);

    @Shadow
    public abstract Matrix4f getBasicProjectionMatrix(double d);


    @Shadow
    protected abstract double getFov(Camera camera, float tickDelta, boolean changingFov);

    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void renderHand(MatrixStack matrixStack, Camera camera, float tickdelta, CallbackInfo info) {
        if (Wrapper.mc.player == null && Wrapper.mc.world == null) return;

        Render3DEvent event = new Render3DEvent(matrixStack, tickdelta);
        Hack.event().post(event);
        if (event.isCancelled()) info.cancel();
    }

    @SuppressWarnings("resource")
    @Inject(method = "renderWorld(FJLnet/minecraft/client/util/math/MatrixStack;)V", at = @At(value = "INVOKE", target = "com/mojang/blaze3d/systems/RenderSystem.clear(IZ)V"))
    private void onRenderWorld(float partialTicks, long finishTimeNano, MatrixStack matrixStack1, CallbackInfo ci) {
        if (MinecraftClient.getInstance().getEntityRenderDispatcher().camera == null)
            return;
        RenderSystem.clearColor(1, 1, 1, 1);
        MatrixStack matrixStack = new MatrixStack();
        double d = this.getFov(camera, partialTicks, true);
        matrixStack.peek().getPositionMatrix().multiply(this.getBasicProjectionMatrix(d));
        loadProjectionMatrix(matrixStack.peek().getPositionMatrix());

        this.bobViewWhenHurt(matrixStack, partialTicks);
        Renderer3D.get.applyCameraRots(matrixStack);
        loadProjectionMatrix(matrixStack.peek().getPositionMatrix());
        Render3DEvent.EventRender3DNoBob eventnobob = new Render3DEvent.EventRender3DNoBob(matrixStack, partialTicks);
        Hack.event().post(eventnobob);
        Renderer3D.get.fixCameraRots(matrixStack);
        if (this.client.options.getBobView().getValue()) {
            bobView(matrixStack, partialTicks);
        }
        Renderer3D.get.applyCameraRots(matrixStack);
        loadProjectionMatrix(matrixStack.peek().getPositionMatrix());
        //EventRenderGetPos eventgetpo EventRenderGetPos(matrixStack, partialTicks).run();
        Renderer3D.get.fixCameraRots(matrixStack);
        loadProjectionMatrix(matrixStack.peek().getPositionMatrix());

        // сделать евент как в метеоре
//        Render3DEvent event = new Render3DEvent(matrixStack1, partialTicks);
//        Hack.event().post(event);
    }

    @Inject(method = "bobViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void onBobViewWhenHurt(MatrixStack matrixStack, float f, CallbackInfo info) {
        if (NoRender.get.isActive() && NoRender.get.hurt.get()) info.cancel();
    }

    @Inject(method = "showFloatingItem", at = @At("HEAD"), cancellable = true)
    private void onShowFloatingItem(ItemStack floatingItem, CallbackInfo info) {
        if (floatingItem.getItem() == Items.TOTEM_OF_UNDYING && NoRender.get.isActive() && NoRender.get.pop.get()) {
            info.cancel();
        }
    }

    @Inject(method = "updateTargetedEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/ProjectileUtil;raycast(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;D)Lnet/minecraft/util/hit/EntityHitResult;"), cancellable = true)
    private void onUpdateTargetedEntity(float tickDelta, CallbackInfo info) {
        if (NoMiningTrace.instance.canWork() && client.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            client.getProfiler().pop();
            info.cancel();
        }
    }
}
