package me.eureka.harvest.gui;

import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.modules.client.ClickGUI.ClickGUI;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.awt.*;
import java.util.ArrayList;

import static me.eureka.harvest.ic.util.Wrapper.mc;

public class Gui extends Screen {
    public static ArrayList<Window> windows = new ArrayList<>();

    public Gui() {
        super(Text.literal("ClickGUI"));

        int x = 5;
        for (Module.Category category : Module.Category.values()) {
            windows.add(new Window(category, x, 3, 95, 15));
            x += ClickGUI.INSTANCE.ears.get() ? 115 : 100;
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float partialTicks) {
        matrices.scale(1.0F, 1.0F, 1.0F);

        DrawableHelper.fill(matrices, 0, 0, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight(), new Color(0, 0, 0, 150).getRGB());
        for (Window window : windows) {
            window.render(mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        windows.forEach(window -> window.mouseDown((int) mouseX, (int) mouseY, mouseButton));
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int state) {
        windows.forEach(window -> window.mouseUp((int) mouseX, (int) mouseY));
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (Window window : windows) {
            window.keyPress(keyCode);
        }

        if (keyCode == 256) {
            mc.setScreen(null);
            ClickGUI.INSTANCE.toggle();
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public void drawGradient(int left, int top, int right, int bottom, int startColor, int endColor) {
        MatrixStack st = new MatrixStack();
        st.scale(1.0F, 1.0F, 1.0F);
        this.fillGradient(st, left, top, right, bottom, startColor, endColor);
    }

    public void drawFlat(int left, int top, int right, int bottom, int startColor) {
        MatrixStack st = new MatrixStack();
        st.scale(1.0F, 1.0F, 1.0F);
        this.fillGradient(st, left, top, right, bottom, startColor, startColor);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (amount < 0) {
            for (Window window : windows) {
                window.y(window.y() - ClickGUI.INSTANCE.scrollFactor.get());
            }
        } else if (amount > 0) {
            for (Window window : windows) {
                window.y(window.y() + ClickGUI.INSTANCE.scrollFactor.get());
            }
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }
}
