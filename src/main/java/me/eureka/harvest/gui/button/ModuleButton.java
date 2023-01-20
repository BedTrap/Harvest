package me.eureka.harvest.gui.button;

import me.eureka.harvest.Hack;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.gui.Component;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.modules.client.ClickGUI.ClickGUI;
import me.eureka.harvest.gui.button.settings.*;
import me.eureka.harvest.systems.modules.client.HUD.HUD;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ModuleButton implements Component {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final Module module;
    private final List<SettingButton> buttons = new ArrayList<>();

    private int x, y;
    private final int w, h;
    private boolean open;
    private int showingModuleCount;

//    private final Color GRADIENT_START = new Color(30, 30, 30, 190);
//    private final Color GRADIENT_END = new Color(25, 25, 25, 190);
    private final Color DARK_GRAY = new Color(30, 30, 30, 150);
    private final Color GRAY = new Color(155, 155, 155, 255);
    private final Color BLACK = new Color(20, 20, 20, 255);
    private final Color WHITE = new Color(255, 255, 255, 255);

    public ModuleButton(Module module, int x, int y, int w, int h) {
        this.module = module;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;

        int n = 0;

        buttons.add(new BindSetting(module, this.x, this.y + this.h + n, this.w, this.h));
        for (Setting<?> setting : Hack.settings().getSettingsForMod(module)) {
            SettingButton settingButton = null;

            switch (setting.getType()) {
                case Boolean -> settingButton = new BooleanSetting(module, setting, this.x, this.y + this.h + n, this.w, this.h);
                case Integer -> settingButton = new IntegerSetting.Slider(module, setting, this.x, this.y + this.h + n, this.w, this.h);
                case Double -> settingButton = new DoubleSetting.Slider(module, setting, this.x, this.y + this.h + n, this.w, this.h);
                case Mode -> settingButton = new ModeSetting(module, setting, this.x, this.y + this.h + n, this.w, this.h);
            }

            buttons.add(settingButton);
            n += this.h;
        }

        buttons.add(new DrawnSetting(module, this.x, this.y + this.h + n, this.w, this.h));
    }

    @Override
    public void render(int mouseX, int mouseY) {
        MatrixStack stack = new MatrixStack();
        stack.scale(1.0F, 1.0F, 1.0F);

        ClickGUI.INSTANCE.drawOutline(stack, x, y, w, h);

        boolean hover = isHover(x, y, w, h - 1, mouseX, mouseY);
        if (module.isActive()) {
            Hack.gui().drawFlat(x + 2, y + 1, x + w - 2, y + h, hover ? HUD.INSTANCE.colorType().darker().getRGB() : HUD.INSTANCE.color());
        } else {
            Hack.gui().drawFlat(x + 2, y + 1, x + w - 2, y + h, hover ? DARK_GRAY.brighter().getRGB() : DARK_GRAY.getRGB());
        }

        mc.textRenderer.drawWithShadow(stack, module.getName(), (float) (x + 5), (float) (y + 4), module.isActive() ? WHITE.getRGB() : GRAY.getRGB());
        mc.textRenderer.drawWithShadow(stack, open ? "-" : "+", (float) (x + 85), (float) (y + 4), module.isActive() ? WHITE.getRGB() : GRAY.getRGB());
    }

    @Override
    public void mouseDown(int mouseX, int mouseY, int button) {
        if (isHover(x, y, w, h - 1, mouseX, mouseY)) {
            switch (button) {
                case 0 -> {
                    mc.player.playSound(SoundEvents.UI_BUTTON_CLICK, 1f, 1f);
                    if (!module.getName().equals(ClickGUI.INSTANCE.name)) module.toggle();
                }
                case 1 -> processRightClick();
            }
        }

        if (open) {
            for (SettingButton settingButton : buttons) {
                settingButton.mouseDown(mouseX, mouseY, button);
            }
        }
    }

    @Override
    public void mouseUp(int mouseX, int mouseY) {
        for (SettingButton settingButton : buttons) {
            settingButton.mouseUp(mouseX, mouseY);
        }
    }

    @Override
    public void keyPress(int key) {
        for (SettingButton settingButton : buttons) {
            settingButton.keyPress(key);
        }
    }

    @Override
    public void close() {
        for (SettingButton button : buttons) {
            button.close();
        }
    }

    private boolean isHover(int x, int y, int w, int h, int mouseX, int mouseY) {
        return mouseX >= x * 1.0F && mouseX <= (x + w) * 1.0F && mouseY >= y * 1.0F && mouseY <= (y + h) * 1.0F;
    }

    public void x(int x) {
        this.x = x;
    }
    public void y(int y) {
        this.y = y;
    }
    public boolean open() {
        return this.open;
    }
    public Module module() {
        return module;
    }
    public List<SettingButton> buttons() {
        return this.buttons;
    }
    public int moduleCount() {
        return this.showingModuleCount;
    }

    public void processRightClick() {
        mc.player.playSound(SoundEvents.UI_BUTTON_CLICK, 1f, 1f);
        if (!open) {
            showingModuleCount = buttons.size();
            open = true;
        } else {
            showingModuleCount = 0;
            open = false;
        }
    }
}
