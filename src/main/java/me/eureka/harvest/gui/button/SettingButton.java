package me.eureka.harvest.gui.button;

import me.eureka.harvest.Hack;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.gui.Component;
import me.eureka.harvest.systems.modules.client.ClickGUI.ClickGUI;
import me.eureka.harvest.systems.modules.client.HUD.HUD;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.Glyph;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Formatting;

import java.awt.*;

public class SettingButton implements Component {
    public final MinecraftClient mc = MinecraftClient.getInstance();
    private Module module;
    private int x, y, w, h;

    protected final int WHITE = new Color(255, 255, 255, 255).getRGB();
    protected final int GRAY = new Color(155, 155, 155, 255).getRGB();
    protected final int BLACK = new Color(20, 20, 20, 255).getRGB();

    public SettingButton(Module module, int x, int y, int w, int h) {
        this.module = module;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    @Override
    public void render(int mouseX, int mouseY) {
    }

    @Override
    public void mouseDown(int mouseX, int mouseY, int button) {
    }

    @Override
    public void mouseUp(int mouseX, int mouseY) {
    }

    @Override
    public void keyPress(int key) {
    }

    @Override
    public void close() {
    }

    public void drawButton() {
        int HUD = new Color(ClickGUI.INSTANCE.r.get(), ClickGUI.INSTANCE.g.get(), ClickGUI.INSTANCE.b.get(), ClickGUI.INSTANCE.alpha.get()).getRGB();
        MatrixStack stack = new MatrixStack();
        stack.scale(1.0F, 1.0F, 1.0F);

        // Background
        DrawableHelper.fill(stack, x() + 3, y(), x() + w() - 3, y() + h(), BLACK);
        // Sides
        DrawableHelper.fill(stack, x() + 3, y(), x() + 2, y() + h(), HUD);
        DrawableHelper.fill(stack, x() + w() - 3, y(), x() + w() - 2, y() + h(), HUD);
    }

    public void drawButton(int mouseX, int mouseY) {
        boolean hover = hover(x(), y(), w(), h() - 1, mouseX, mouseY);
        Hack.gui().drawFlat(x() + 3, y() + 1, x() + w() - 3, y() + h(), hover ? HUD.INSTANCE.colorType().darker().getRGB() : HUD.INSTANCE.color());
    }

    public void drawDescription(int mouseX, int mouseY, String settingDescription, SettingButton clazz) {
        if (!hover(x(), y(), w(), h() - 1, mouseX, mouseY)) return;

        String description = settingDescription == null ? "A Setting. (" + clazz.getClass().getSimpleName().replace("Setting", "") + ")" : settingDescription;

        int x = mc.getWindow().getScaledWidth() - mc.textRenderer.getWidth(description);
        int y = mc.getWindow().getScaledHeight() - 10;

        DrawableHelper.fill(new MatrixStack(), x - 2, y - 2, (int) (x + mc.textRenderer.getWidth(description) + 1 * 1.5D), y + mc.textRenderer.fontHeight + 2, new Color(50, 50, 50).getRGB());
        mc.textRenderer.drawWithShadow(new MatrixStack(), description, x, y, -1);
    }

    public int height() {
        return h;
    }
    public Module module() {
        return this.module;
    }
    public void module(Module module) {
        this.module = module;
    }
    public boolean drawn() {
        return this.module().drawn;
    }
    public void drawn(boolean drawn) {
        this.module().drawn = drawn;
    }

    public int x() {return x;}
    public void x(int x) {
        this.x = x;}
    public int y() {return y;}
    public void y(int y) {
        this.y = y;
    }
    public int w() {return w;}
    public void w(int w) {
        this.w = w;
    }
    public int h() {return h;}
    public void h(int h) {
        this.h = h;
    }

    public boolean hover(int X, int Y, int W, int H, int mouseX, int mouseY) {
        return mouseX >= X * 1.0F && mouseX <= (X + W) * 1.0F && mouseY >= Y * 1.0F && mouseY <= (Y + H) * 1.0F;
    }
}
