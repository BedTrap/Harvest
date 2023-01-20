package me.eureka.harvest.mixins;

import me.eureka.harvest.Hack;
import me.eureka.harvest.events.event.OpenScreenEvent;
import me.eureka.harvest.events.event.TickEvent;
import me.eureka.harvest.ic.util.Wrapper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.util.Window;
import net.minecraft.util.crash.CrashReport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;


@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient {
    @Shadow
    public abstract void scheduleStop();

    @Shadow
    @Nullable
    public ClientPlayerInteractionManager interactionManager;

    @Inject(method = "tick", at = @At("HEAD"))
    private void beforeTick(CallbackInfo info) {
        if (Wrapper.mc.player == null && Wrapper.mc.world == null) return;

        TickEvent.Pre event = new TickEvent.Pre();
        Hack.event().post(event);
    }

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void onOpenScreen(Screen screen, CallbackInfo info) {
        OpenScreenEvent event = new OpenScreenEvent(screen);
        Hack.event().post(event);

        if (event.isCancelled()) info.cancel();
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;pop()V", ordinal = 0, shift = At.Shift.AFTER), cancellable = true)
    private void onTick(CallbackInfo info) {
        if (Wrapper.mc.player != null && Wrapper.mc.world != null) {
            TickEvent.Post event = new TickEvent.Post();
            Hack.event().post(event);
        }
    }

    @Inject(method = "getWindowTitle", at = @At("HEAD"), cancellable = true)
    public void getWindowTitle(CallbackInfoReturnable<String> ci) {
        ci.setReturnValue(Hack.NAME + " " + Hack.VERSION);
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/Window;setIcon(Ljava/io/InputStream;Ljava/io/InputStream;)V"))
    public void setAlternativeWindowIcon(Window window, InputStream inputStream1, InputStream inputStream2)
            throws IOException {
        window.setIcon(Hack.class.getResourceAsStream("/assets/picture/harvest.png"), Hack.class.getResourceAsStream("/assets/picture/harvest.png"));
    }
}