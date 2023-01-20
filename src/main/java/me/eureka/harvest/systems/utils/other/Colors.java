package me.eureka.harvest.systems.utils.other;

import net.minecraft.util.Formatting;

import java.awt.*;
import java.util.List;

public class Colors {
    public static final List<String> colors = List.of("Red", "Green", "Blue", "Yellow", "Magenta", "Cyan", "White", "Black", "Gray", "Light Gray", "Dark Gray", "Pink", "Orange", "None");

    public static Formatting getFormatting(String color) {
        return switch (color) {
            case "RED", "Red", "red" -> Formatting.RED;
            case "AQUA", "Aqua", "aqua" -> Formatting.AQUA;
            case "BLUE", "Blue", "blue" -> Formatting.BLUE;
            case "DARK_BLUE", "DARKBLUE", "DarkBlue", "darkblue" -> Formatting.DARK_BLUE;
            case "DARK_GREEN", "DARKGREEN", "DarkGreen", "darkgreen" -> Formatting.DARK_GREEN;
            case "DARK_AQUA", "DARKAQUA", "DarkAqua", "darkaqua" -> Formatting.DARK_AQUA;
            case "DARK_RED", "DARKRED", "DarkRed", "darkred" -> Formatting.DARK_RED;
            case "DARK_PURPLE", "DARKPURPLE", "DarkPurple", "darkpurple" -> Formatting.DARK_PURPLE;
            case "GOLD", "Gold", "gold" -> Formatting.GOLD;
            case "GRAY", "Gray", "gray" -> Formatting.GRAY;
            case "BLACK", "Black", "black" -> Formatting.BLACK;
            case "GREEN", "Green", "green" -> Formatting.GREEN;
            case "WHITE", "White", "white" -> Formatting.WHITE;
            case "DARK_GRAY", "DARKGRAY", "DarkGray", "darkgray" -> Formatting.DARK_GRAY;
            case "LIGHT_PURPLE", "LIGHTPURPLE", "LightPurple", "lightpurple" -> Formatting.LIGHT_PURPLE;
            case "YELLOW", "Yellow", "yellow" -> Formatting.YELLOW;
            default -> null;
        };
    }

    public static String getString(Formatting formatting) {
        return switch (formatting) {
            case RED -> "red";
            case AQUA -> "aqua";
            case BLUE -> "blue";
            case DARK_BLUE -> "darkblue";
            case DARK_GREEN -> "darkgreen";
            case DARK_AQUA -> "darkaqua";
            case DARK_RED -> "darkred";
            case DARK_PURPLE -> "darkpurple";
            case GOLD -> "gold";
            case GRAY -> "gray";
            case BLACK -> "black";
            case GREEN -> "green";
            case WHITE -> "white";
            case DARK_GRAY -> "darkgray";
            case LIGHT_PURPLE -> "lightpurple";
            case YELLOW -> "yellow";
            default -> null;
        };
    }

    public static Color getColor(String color) {
        return switch (color) {
            case "Red" -> Color.RED;
            case "Green" -> Color.GREEN;
            case "Blue" -> Color.BLUE;
            case "Yellow" -> Color.YELLOW;
            case "Magenta" -> Color.MAGENTA;
            case "Cyan" -> Color.CYAN;
            case "White" -> Color.WHITE;
            case "Gray" -> Color.GRAY;
            case "Light Gray" -> Color.LIGHT_GRAY;
            case "Dark Gray" -> Color.DARK_GRAY;
            case "Pink" -> Color.PINK;
            case "Orange" -> Color.ORANGE;
            default -> Color.BLACK;
        };
    }
}
