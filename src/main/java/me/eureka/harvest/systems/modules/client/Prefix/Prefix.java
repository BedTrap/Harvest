package me.eureka.harvest.systems.modules.client.Prefix;

import me.eureka.harvest.Hack;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.systems.managers.RainbowManager;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.modules.client.HUD.HUD;
import me.eureka.harvest.systems.utils.game.ChatUtils;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Module.Info(name = "Prefix", category = Module.Category.Client)
public class Prefix extends Module {
    public Setting<Boolean> prefix = register("Prefix", true);
    public Setting<Boolean> module = register("Module", true);
    public Setting<Boolean> brackets = register("Brackets", true);
    public Setting<Integer> desync = register("Desync", 3, 0, 10);
    public Setting<Integer> speed = register("Speed", 30, 0, 50);

    // Holy sheit guys, i have no idea how to make it look more readable
    // it works dont touch it
    // btw this is a second 1.19.2 client with rainbow prefix, the first was vh
    public MutableText getPrefix(String message) {
        MutableText text = Text.literal("");
        AtomicInteger offset = new AtomicInteger();
        text.append(Text.literal("[").setStyle(Style.EMPTY.withColor(brackets.get() ? RainbowManager.INSTANCE.getRainbowColor(offset.addAndGet(desync.get())) : HUD.INSTANCE.color())));

        if (this.prefix.get()) {
            char[] chars = Hack.NAME.toCharArray();

            List.of(chars).forEach(c -> text.append(Text.literal(String.valueOf(c)).setStyle(Style.EMPTY.withColor(RainbowManager.INSTANCE.getRainbowColor(offset.addAndGet(this.desync.get()))))));
        } else text.append(Text.literal(Hack.NAME).setStyle(Style.EMPTY.withColor(HUD.INSTANCE.color())));

        text.append(Text.literal("]").setStyle(Style.EMPTY.withColor(brackets.get() ? RainbowManager.INSTANCE.getRainbowColor(offset.addAndGet(desync.get())) : HUD.INSTANCE.color())));
        text.append(Text.literal(" "));

        if (ChatUtils.hasModule(message) && module.get()) {
            String[] split = message.split(" ");
            String moduleName = split[1].replace("[", "").replace("]", "");
            text.append(Text.literal("[").setStyle(Style.EMPTY.withColor(brackets.get() ? RainbowManager.INSTANCE.getRainbowColor(offset.addAndGet(desync.get())) : HUD.INSTANCE.color())));
            char[] chars = moduleName.toCharArray();

            List.of(chars).forEach(c -> text.append(Text.literal(String.valueOf(c)).setStyle(Style.EMPTY.withColor(RainbowManager.INSTANCE.getRainbowColor(offset.addAndGet(this.desync.get()))))));
            text.append(Text.literal("] ").setStyle(Style.EMPTY.withColor(brackets.get() ? RainbowManager.INSTANCE.getRainbowColor(offset.get() + desync.get()) : HUD.INSTANCE.color())));
        }

        return text;
    }

    public static Prefix INSTANCE;

    public Prefix() {
        INSTANCE = this;
    }
}
