package me.eureka.harvest.gui.button.settings;

import me.eureka.harvest.Hack;
import me.eureka.harvest.events.event.StateEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.gui.button.SettingButton;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.modules.client.ClickGUI.ClickGUI;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;

public class BooleanSetting extends SettingButton {
    private final Setting<Boolean> setting;

    public BooleanSetting(Module module, Setting setting, int x, int y, int w, int h) {
        super(module, x, y, w, h);
        this.setting = setting;
    }

    @Override
    public void render(int mouseX, int mouseY) {
        drawButton();
        drawDescription(mouseX, mouseY, this.setting.getDescription(), this);

        MatrixStack stack = new MatrixStack();
        stack.scale(1.0F, 1.0F, 1.0F);

        Hack.gui().drawFlat(x() + 3, y() + 1, x() + w() - 3, y() + h(), BLACK);
        ClickGUI.INSTANCE.drawCheckBox(setting.get(), x() + w() - 13, y() + 3);

        mc.textRenderer.drawWithShadow(stack, setting.getName(), (float) (x() + 6), (float) (y() + 4), setting.get() ? WHITE : GRAY);
    }

    @Override
    public void mouseDown(int mouseX, int mouseY, int button) {
        if (hover(x(), y(), w(), h() - 1, mouseX, mouseY) && button == 0) {
            mc.player.playSound(SoundEvents.UI_BUTTON_CLICK, 1f, 1f);

            setting.setValue(!setting.get());
            Hack.event().post(new StateEvent.Button(this.module(), setting));
        }
    }
}
