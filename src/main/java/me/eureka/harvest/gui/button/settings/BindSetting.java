package me.eureka.harvest.gui.button.settings;

import me.eureka.harvest.gui.button.SettingButton;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.modules.client.ClickGUI.ClickGUI;
import me.eureka.harvest.systems.utils.key.KeyName;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;

import java.awt.*;

public class BindSetting extends SettingButton {
    private final Module module;
    private boolean binding;

    public BindSetting(Module module, int x, int y, int w, int h) {
        super(module, x, y, w, h);
        this.module = module;
    }

    @Override
    public void render(int mouseX, int mouseY) {
        drawButton();
        drawButton(mouseX, mouseY);

        MatrixStack stack = new MatrixStack();
        stack.scale(1.0F, 1.0F, 1.0F);

        int color = new Color(ClickGUI.INSTANCE.r.get(), ClickGUI.INSTANCE.g.get(), ClickGUI.INSTANCE.b.get(), ClickGUI.INSTANCE.alpha.get()).getRGB();
        ClickGUI.INSTANCE.drawLine(stack, x(), y(), w(), h(), color);

        mc.textRenderer.drawWithShadow(stack, "Bind", (float) (x() + 6), (float) (y() + 4), WHITE);
        mc.textRenderer.drawWithShadow(stack, binding ? "..." : KeyName.get(module.getBind()), (float) ((x() + 6) + mc.textRenderer.getWidth(" Bind")), (float) (y() + 4), GRAY);
    }

    @Override
    public void mouseDown(int mouseX, int mouseY, int button) {
        if (hover(x(), y(), w(), h() - 1, mouseX, mouseY)) {
            mc.player.playSound(SoundEvents.UI_BUTTON_CLICK, 1f, 1f);
            binding = !binding;
        }
    }

    @Override
    public void keyPress(int key) {
        if (binding) {
            if (key == 256 || key == 259 || key == 261) {
                this.module().setBind(-1);
            } else module().setBind(key);

            binding = false;
        }
    }
}
