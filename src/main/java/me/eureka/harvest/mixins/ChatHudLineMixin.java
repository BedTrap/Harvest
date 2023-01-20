package me.eureka.harvest.mixins;

import me.eureka.harvest.mixininterface.IChatHudLine;
import me.eureka.harvest.systems.modules.render.NoRender.NoRender;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = { ChatHudLine.class, ChatHudLine.Visible.class })
public class ChatHudLineMixin implements IChatHudLine {
    @Unique private int id;

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }
}