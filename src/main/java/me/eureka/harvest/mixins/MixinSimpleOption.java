package me.eureka.harvest.mixins;

import me.eureka.harvest.mixininterface.ISimpleOption;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SimpleOption.class)
public class MixinSimpleOption<T> implements ISimpleOption<T> {
    @Shadow
    T value;

    @Override
    public void forceSetValue(T newValue) {
        value = newValue;
    }
}