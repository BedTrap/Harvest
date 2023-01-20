package me.eureka.harvest.gui.button.settings;

import me.eureka.harvest.Hack;
import me.eureka.harvest.gui.button.SettingButton;
import me.eureka.harvest.systems.modules.Module;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;

import java.awt.*;

public class DrawnSetting extends SettingButton {
    public DrawnSetting(Module module, int x, int y, int w, int h) {
        super(module, x, y, w, h);
    }

    @Override
    public void render(int mouseX, int mouseY) {
        drawButton();

        MatrixStack stack = new MatrixStack();
        stack.scale(1.0F, 1.0F, 1.0F);

        if (this.drawn()) {
            drawButton(mouseX, mouseY);
        } else {
            boolean hover = hover(x(), y(), w(), h() - 1, mouseX, mouseY);
            Hack.gui().drawFlat(x() + 3, y() + 1, x() + w() - 3, y() + h(), hover ? new Color(BLACK).brighter().getRGB() : BLACK);
        }

        mc.textRenderer.drawWithShadow(stack, "Drawn", (float) (x() + 6), (float) (y() + 4), this.module().drawn ? WHITE : GRAY);
    }

    @Override
    public void mouseDown(int mouseX, int mouseY, int button) {
        if (hover(x(), y(), w(), h() - 1, mouseX, mouseY) && (button == 0 || button == 1)) {
            mc.player.playSound(SoundEvents.UI_BUTTON_CLICK, 1f, 1f);
            this.drawn(!this.drawn());
        }
    }
}
