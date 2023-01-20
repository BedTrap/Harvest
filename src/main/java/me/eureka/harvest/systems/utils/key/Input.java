package me.eureka.harvest.systems.utils.key;

import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

import static me.eureka.harvest.ic.util.Wrapper.mc;

public class Input {
    private static final boolean[] keys = new boolean[512];
    private static final boolean[] buttons = new boolean[16];


    public static void setKeyState(int key, boolean pressed) {
        if (key >= 0 && key < keys.length) keys[key] = pressed;
    }

    public static void setButtonState(int button, boolean pressed) {
        if (button >= 0 && button < buttons.length) buttons[button] = pressed;
    }

    public static void setKeyState(KeyBinding bind, boolean pressed) {
        setKeyState(KeyBinds.getKey(bind), pressed);
    }

    public static boolean isPressed(KeyBinding bind) {
        return isKeyPressed(KeyBinds.getKey(bind));
    }

    public static boolean isKeyPressed(int key) {
        if (key == GLFW.GLFW_KEY_UNKNOWN) return false;
        return key < keys.length && keys[key];
    }

    public static boolean isButtonPressed(int button) {
        if (button == -1) return false;
        return button < buttons.length && buttons[button];
    }

    public static boolean mouseIsOver(double mouseX, double mouseY, Rectangle rect){
        return mouseX >= rect.x && mouseX <= rect.width + rect.x && mouseY >= rect.y && mouseY <= rect.height + rect.y;
    }

    public static int getMouseX() {
        return (int) (mc.mouse.getX() / mc.getWindow().getScaleFactor());
    }

    public static int getMouseY() {
        return (int) (mc.mouse.getY() / mc.getWindow().getScaleFactor());
    }
}