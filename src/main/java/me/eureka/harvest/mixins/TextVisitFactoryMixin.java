package me.eureka.harvest.mixins;

import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import net.minecraft.client.font.TextVisitFactory;

import java.util.Random;

import static me.eureka.harvest.ic.util.Wrapper.mc;

@Mixin(TextVisitFactory.class)
public abstract class TextVisitFactoryMixin {
    @ModifyArg(at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/font/TextVisitFactory;visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z",
            ordinal = 0),
            method = {"visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z"},
            index = 0)
    private static String adjustText(String text) {
        return text;
    }
}