package me.eureka.harvest.mixins;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;


@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {

    @Accessor("itemUseCooldown")
    void setItemUseCooldown(int itemUseCooldown);

    @Accessor("currentFps")
    static int getFps() {
        return 0;
    }

    @Accessor("itemUseCooldown")
    int getItemUseCooldown();

    @Invoker("doAttack")
    boolean leftClick();
}