package me.eureka.harvest.gui.button.settings;

import me.eureka.harvest.Hack;
import me.eureka.harvest.events.event.StateEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.gui.button.SettingButton;
import me.eureka.harvest.systems.modules.Module;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;

public class ModeSetting extends SettingButton {
    private final Setting<String> setting;

    public ModeSetting(Module module, Setting setting, int x, int y, int w, int h) {
        super(module, x, y, w, h);
        this.setting = setting;
    }

    @Override
    public void render(int mouseX, int mouseY) {
        drawButton();
        drawDescription(mouseX, mouseY, this.setting.getDescription(), this);

        MatrixStack stack = new MatrixStack();
        stack.scale(1.0F, 1.0F, 1.0F);

        drawButton(mouseX, mouseY);
        mc.textRenderer.drawWithShadow(stack, setting.getName(), (float) (x() + 6), (float) (y() + 4), WHITE);
        mc.textRenderer.drawWithShadow(stack, setting.get(), (float) ((x() + 6) + mc.textRenderer.getWidth(" " + setting.getName())), (float) (y() + 4), GRAY);
    }

    @Override
    public void mouseDown(int mouseX, int mouseY, int button) {
        if (hover(x(), y(), w(), h() - 1, mouseX, mouseY)) {
            mc.player.playSound(SoundEvents.UI_BUTTON_CLICK, 1f, 1f);
            if (button == 0) {
                int i = 0;
                int enumIndex = 0;
                for (String enumName : setting.getModes()) {
                    if (enumName.equals(setting.get()))
                        enumIndex = i;
                    i++;
                }
                if (enumIndex == setting.getModes().size() - 1) {
                    String mode = setting.getModes().get(0);
                    setting.setValue(mode);

                    Hack.event().post(new StateEvent.Mode(this.module(), mode));
                } else {
                    enumIndex++;
                    i = 0;
                    for (String enumName : setting.getModes()) {
                        if (i == enumIndex) {
                            setting.setValue(enumName);
                            Hack.event().post(new StateEvent.Mode(this.module(), enumName));
                        }
                        i++;
                    }
                }
            } else if (button == 1) {
                int i = 0;
                int enumIndex = 0;
                for (String enumName : setting.getModes()) {
                    if (enumName.equals(setting.get()))
                        enumIndex = i;
                    i++;
                }
                if (enumIndex == 0) {
                    String mode = setting.getModes().get(setting.getModes().size() - 1);
                    setting.setValue(mode);

                    Hack.event().post(new StateEvent.Mode(this.module(), mode));
                } else {
                    enumIndex--;
                    i = 0;
                    for (String enumName : setting.getModes()) {
                        if (i == enumIndex) {
                            setting.setValue(enumName);
                            Hack.event().post(new StateEvent.Mode(this.module(), enumName));
                        }
                        i++;
                    }
                }
            }
        }
    }
}
