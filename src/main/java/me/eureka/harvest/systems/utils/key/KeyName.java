package me.eureka.harvest.systems.utils.key;

import net.minecraft.client.util.InputUtil;

public class KeyName {
    public static String get(int keyCode) {
        return switch (keyCode) {
            case -1 -> "NONE";
            default -> InputUtil.fromKeyCode(keyCode, -1).getLocalizedText().getString();
        };
    }
}
