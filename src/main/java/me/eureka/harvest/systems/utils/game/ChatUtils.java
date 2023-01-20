package me.eureka.harvest.systems.utils.game;

import me.eureka.harvest.Hack;
import me.eureka.harvest.mixininterface.IChatHud;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.modules.client.ClickGUI.ClickGUI;
import me.eureka.harvest.systems.modules.client.HUD.HUD;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Iterator;

import static me.eureka.harvest.ic.util.Wrapper.mc;

public class ChatUtils {
    private static Text PREFIX;
    private static final int LOOP = 0;
    private static final int ONCE = 86741;

    public static void info(String text) {
        if (mc.world == null) return;

        PREFIX = Text.literal("[" + Hack.NAME + "] ").setStyle(Style.EMPTY.withColor(HUD.INSTANCE.color()));

        MutableText message = Text.literal("");
        message.append(PREFIX);
        message.setStyle(message.getStyle().withFormatting(Formatting.GRAY));
        message.append(text);

        ((IChatHud) mc.inGameHud.getChatHud()).add(message, LOOP);
    }

    public static void info(String name, String text) {
        info(name, text, true);
    }

    public static void info(String name, Text text) {
        info(name, text, true);
    }

    public static void info(String name, String text, boolean feedback) {
        if (mc.world == null) return;
        Text MODULE;

        PREFIX = Text.literal("[" + Hack.NAME + "] ")
                .setStyle(Style.EMPTY.withColor(HUD.INSTANCE.color()));
        MODULE = Text.literal("[" + name + "] ")
                .setStyle(Style.EMPTY.withColor(HUD.INSTANCE.revertColor()));

        MutableText message = Text.literal("");
        message.append(PREFIX);
        message.append(MODULE);
        message.setStyle(message.getStyle().withFormatting(Formatting.GRAY));
        message.append(text);

        ((IChatHud) mc.inGameHud.getChatHud()).add(message, feedback ? LOOP : ONCE);
    }

    public static void info(String name, Text text, boolean feedback) {
        if (mc.world == null) return;
        Text MODULE;

        PREFIX = Text.literal("[" + Hack.NAME + "] ")
                .setStyle(Style.EMPTY.withColor(HUD.INSTANCE.color()));
        MODULE = Text.literal("[" + name + "] ")
                .setStyle(Style.EMPTY.withColor(HUD.INSTANCE.revertColor()));

        MutableText message = Text.literal("");
        message.append(PREFIX);
        message.append(MODULE);
        message.setStyle(message.getStyle().withFormatting(Formatting.GRAY));
        message.append(text);

        ((IChatHud) mc.inGameHud.getChatHud()).add(message, feedback ? LOOP : ONCE);
    }

    public void infoModule(String name, boolean isActive) {
        if (mc.world == null) return;
        PREFIX = Text.literal("[" + Hack.NAME + "] ")
                .setStyle(Style.EMPTY.withColor(HUD.INSTANCE.color()));

        if (name.equals(ClickGUI.INSTANCE.name)) return;

        MutableText message = Text.literal("");
        message.append(PREFIX);
        message.append(Formatting.GRAY + name + " toggled ");
        message.append(isActive ? Formatting.GREEN + "on" + Formatting.GRAY + "." : Formatting.RED + "off" + Formatting.GRAY + ".");

        try {
            ((IChatHud) mc.inGameHud.getChatHud()).add(message, ONCE);
        } catch (NullPointerException e) {
            System.out.println("Something went wrong, NullPointerException was caught in infoModule(" + name + ", " + isActive + ");");
        }
    }

    public static String asInfoModule(String text) {
        String[] split = text.split(" ");
        StringBuilder textBuilder = new StringBuilder();

        for (String value : split) {
            String s = value;
            if (s.startsWith("[Harvest]")) {
                textBuilder.append(Formatting.GRAY);
            } else {
                if (s.equals("off.") || s.equals("on.")) {
                    s = s.equals("off.") ? Formatting.RED + "off" + Formatting.GRAY + "." : Formatting.GREEN + "on" + Formatting.GRAY + ".";
                }

                textBuilder.append(s).append(!s.endsWith(".") ? " " : "");
            }
        }

        return textBuilder.toString();
    }

    public static Text putModule(String text) {
        String[] split = text.split(" ");
        String name = split[1];
        MutableText mutableText = Text.literal(name + " ").setStyle(Style.EMPTY.withColor(HUD.INSTANCE.revertColor()));
        StringBuilder textBuilder = new StringBuilder();

        for (String s : split) {
            if (!s.equals("[Harvest]") && !s.equals(name)) {
                textBuilder.append(s).append(!s.endsWith(".") ? " " : "");
            } else {
                textBuilder.append(Formatting.GRAY);
            }
        }

        return mutableText.append(textBuilder.toString());
    }

    public static boolean hasModuleInfo(String text) {
        String[] split = text.split(" ");
        String name = split[1];
        if (!name.startsWith("[") && !name.endsWith("]")) {
            return false;
        } else {
            name = name.replace("[", "").replace("]", "");
            Iterator<Module> var3 = Hack.modules().get().iterator();

            Module module;
            do {
                if (!var3.hasNext()) {
                    return false;
                }

                module = var3.next();
            } while (!module.name.equals(name));

            return true;
        }
    }

    public static boolean hasModule(String text) {
        String[] split = text.split(" ");
        String name = split[1];
        if (!name.startsWith("[") && !name.endsWith("]")) {
            return false;
        } else {
            name = name.replace("[", "").replace("]", "");
            Iterator<Module> var3 = Hack.modules().get().iterator();

            Module module;
            do {
                if (!var3.hasNext()) {
                    return false;
                }

                module = (Module)var3.next();
            } while(!module.name.equals(name));

            return true;
        }
    }
}
