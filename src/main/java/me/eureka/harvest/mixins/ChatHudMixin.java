package me.eureka.harvest.mixins;

import me.eureka.harvest.mixininterface.IChatHud;
import me.eureka.harvest.mixininterface.IChatHudLine;
import me.eureka.harvest.systems.modules.client.Prefix.Prefix;
import me.eureka.harvest.systems.utils.game.ChatUtils;
import me.eureka.harvest.systems.utils.other.StringCharacterVisitor;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin implements IChatHud {
    @Mutable @Shadow @Final private List<ChatHudLine.Visible> visibleMessages;
    @Unique private int nextId;

    @Override
    public void add(Text message, int id) {
        nextId = id;
        addMessage(message);
        nextId = 0;
    }

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;ILnet/minecraft/client/gui/hud/MessageIndicator;Z)V", at = @At(value = "INVOKE", target = "Ljava/util/List;add(ILjava/lang/Object;)V", ordinal = 0, shift = At.Shift.AFTER))
    private void onAddMessageAfterNewChatHudLineVisible(Text message, MessageSignatureData signature, int ticks, MessageIndicator indicator, boolean refresh, CallbackInfo info) {
        ((IChatHudLine) (Object) visibleMessages.get(0)).setId(nextId);
    }

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;ILnet/minecraft/client/gui/hud/MessageIndicator;Z)V", at = @At(value = "INVOKE", target = "Ljava/util/List;add(ILjava/lang/Object;)V", ordinal = 1, shift = At.Shift.AFTER))
    private void onAddMessageAfterNewChatHudLine(Text message, MessageSignatureData signature, int ticks, MessageIndicator indicator, boolean refresh, CallbackInfo info) {
        ((IChatHudLine) (Object) messages.get(0)).setId(nextId);
    }

    @Redirect(method = {"render(Lnet/minecraft/client/util/math/MatrixStack;I)V"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHudLine$Visible;content()Lnet/minecraft/text/OrderedText;"))
    private OrderedText redirect(ChatHudLine.Visible instance) {
        StringCharacterVisitor visitor = new StringCharacterVisitor();
        instance.content().accept(visitor);
        if (Prefix.INSTANCE.isActive() && visitor.result.toString().startsWith("[Harvest] ")) {
            MutableText text = Prefix.INSTANCE.getPrefix(visitor.result.toString());
            if (ChatUtils.hasModuleInfo(visitor.result.toString())) {
                text.append(ChatUtils.putModule(visitor.result.toString()));
            } else {
                text.append(ChatUtils.asInfoModule(visitor.result.toString()));
            }

            return text.asOrderedText();
        } else {
            return instance.content();
        }
    }

/*    // TODO: 12.09.2022 redirect OrderedText instead of rendering text (causes chatType crash)
    @Redirect(method = "render(Lnet/minecraft/client/util/math/MatrixStack;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/TextRenderer;drawWithShadow(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/text/OrderedText;FFI)I"))
    private int redirect(TextRenderer instance, MatrixStack matrices, OrderedText text, float x, float y, int color) {
        StringCharacterVisitor visitor = new StringCharacterVisitor();
        text.accept(visitor);

        int prefixLength = "[Harvest] ".length();
        boolean isClient = visitor.result.toString().startsWith("[Harvest] ");
        if (Prefix.INSTANCE.isActive() && isClient) {
            MutableText prefix = Prefix.INSTANCE.getPrefix();
            MutableText message = crop(asText(text), prefixLength);

            prefix.append(message);
            return instance.drawWithShadow(matrices, prefix, x, y, color);
        }

        return instance.drawWithShadow(matrices, text, x, y, color);
    }*/

    // TODO: 12.09.2022 separate words in text with space, it would be better for editing text for prefix 
    private MutableText asText(OrderedText orderedText) {
        MutableText parsedText = Text.empty();
        orderedText.accept((i, style, codePoint) -> {
            parsedText.append(Text.literal(new String(Character.toChars(codePoint))).setStyle(style));
            return true;
        });
        return parsedText;
    }

    @Shadow
    public abstract void addMessage(Text message);

    @Shadow
    @Final
    private List<ChatHudLine> messages;
}