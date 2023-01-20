package me.eureka.harvest.systems.modules.client.ClickGUI;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import me.eureka.harvest.Hack;
import me.eureka.harvest.events.event.TickEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.systems.modules.Module;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.util.List;

@Module.Info(name = "ClickGUI", category = Module.Category.Client, bind = 345 /* Right Ctrl */, drawn = false)
public class ClickGUI extends Module {
    public Setting<Integer> r = register("Red", 144, 0, 255);
    public Setting<Integer> g = register("Green", 144, 0, 255);
    public Setting<Integer> b = register("Blue", 144, 0, 255);
    public Setting<Boolean> ears = register("Ears", true);
    public Setting<Boolean> checkbox = register("Checkbox", true);
    public Setting<Integer> sliderHeight = register("SHeight", 14, 1, 14);
    public Setting<Boolean> sliderBackground = register("SBackground", true);
    public Setting<Integer> scrollFactor = register("ScrollFactor", 5, 0, 30);
    public Setting<String> outline = register("Outline", List.of("Normal", "Rounded", "OFF"), "Rounded");
    public Setting<Integer> alpha = register("Alpha", 144, 0, 255);

    @Override
    public void onActivate() {
        if (cantUpdate()) return;
        mc.setScreen(Hack.gui());
    }

    @Override
    public void onDeactivate() {
        if (cantUpdate()) return;
        mc.setScreen(null);
    }

    public void drawCheckBox(boolean checked, int x, int y) {
        if (!checkbox.get()) return;

        MatrixStack stacks = new MatrixStack();
        stacks.scale(1.0F, 1.0F, 1.0F);
        Identifier identifier = new Identifier("picture", checked ? "checked.png" : "unchecked.png");
        RenderSystem.setShaderTexture(0, identifier);
        DrawableHelper.drawTexture(stacks, x, y, 0, 0, 10, 10, 10, 10);
    }

    public void drawEar(boolean left, int x, int y) {
        if (!ears.get()) return;

        MatrixStack stack = new MatrixStack();
        stack.scale(1.0F, 1.0F, 1.0F);
        Identifier ear = new Identifier("picture", left ? "left_ear.png" : "right_ear.png");
        RenderSystem.setShaderTexture(0, ear);
        DrawableHelper.drawTexture(stack, x, y, 0, 0, 20, 20, 20, 20);
    }

    public void drawOutline(MatrixStack stack, int x, int y, int w, int h) {
        int color = new Color(r.get(), g.get(), b.get(), alpha.get()).getRGB();

        switch (outline.get()) {
            case "Normal" -> normalOutline(stack, x, y, w, h, color);
            case "Rounded" -> roundedOutline(stack, x, y, w, h, color);
        }
    }

    private void roundedOutline(MatrixStack stack, int x, int y, int w, int h, int color) {
        DrawableHelper.fill(stack, x + 1, y + 1, x + 2, y + h, color);
        drawLine(stack, x, y, w, h, color);
        DrawableHelper.fill(stack, x + w - 1, y + 1, x + w - 2, y + h, color);
        DrawableHelper.fill(stack, x + 2, y + h, x + w - 2, y + h + 1, color);
    }

    private void normalOutline(MatrixStack stack, int x, int y, int w, int h, int color) {
        DrawableHelper.fill(stack, x + 1, y + 1, x + 2, y + h, color);
        drawLine(stack, x, y, w, h, color);
        DrawableHelper.fill(stack, x + w - 1, y + 1, x + w - 2, y + h + 1, color);
        DrawableHelper.fill(stack, x + 1, y + h, x + w - 2, y + h + 1, color);
    }

    public void drawLine(MatrixStack stack, int x, int y, int w, int h, int color) {
        switch (outline.get()) {
            case "Normal" -> DrawableHelper.fill(stack, x + 1, y + 1, x + w - 1, y, color);
            case "Rounded" -> DrawableHelper.fill(stack, x + 2, y + 1, x + w - 2, y, color);
        }
    }

    public static ClickGUI INSTANCE;

    public ClickGUI() {
        INSTANCE = this;
    }
}
