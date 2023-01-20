package me.eureka.harvest.gui.button.settings;

import me.eureka.harvest.Hack;
import me.eureka.harvest.gui.button.SettingButton;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.modules.client.ClickGUI.ClickGUI;
import me.eureka.harvest.systems.modules.client.HUD.HUD;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;

public class DoubleSetting extends SettingButton {
    private final Setting<Double> setting;
    protected boolean dragging;
    protected double sliderWidth;

    public DoubleSetting(Module module, Setting setting, int x, int y, int w, int h) {
        super(module, x, y, w, h);
        this.dragging = false;
        this.sliderWidth = 0;
        this.setting = setting;
    }

    protected void updateSlider(int mouseX) {
    }

    @Override
    public void render(int mouseX, int mouseY) {
        updateSlider(mouseX);

        drawButton();
        drawDescription(mouseX, mouseY, this.setting.getDescription(), this);

        MatrixStack stack = new MatrixStack();
        stack.scale(1.0F, 1.0F, 1.0F);

        // Draws background
        boolean hover = hover(x(), y(), w(), h() - 1, mouseX, mouseY);
        drawBackground(ClickGUI.INSTANCE.sliderBackground.get());
        Hack.gui().drawFlat(x() + 3, y() + ClickGUI.INSTANCE.sliderHeight.get(), (int) (x() - 2 + (sliderWidth) + 5), y() + h(), hover ? HUD.INSTANCE.colorType().darker().getRGB() : HUD.INSTANCE.color());

        mc.textRenderer.drawWithShadow(stack, setting.getName(), (float) (x() + 6), (float) (y() + 4), WHITE);
        mc.textRenderer.drawWithShadow(stack, String.format("%." + setting.getInc() + "f", setting.get()).replace(",", "."), (float) ((x() + 6) + mc.textRenderer.getWidth(" " + setting.getName())), (float) (y() + 4), GRAY);

    }

    private void drawBackground(boolean background) {
        if (background) {
            Hack.gui().drawFlat(x() + 3, y() + 1, x() + w() - 3, y() + h(), BLACK);
        } else {
            Hack.gui().drawFlat((int) (x() + 3 + (sliderWidth)), y() + ClickGUI.INSTANCE.sliderHeight.get(), x() + w() - 3, y() + h(), BLACK);
        }
    }

    @Override
    public void mouseDown(int mouseX, int mouseY, int button) {
        if (hover(x(), y(), w(), h() - 1, mouseX, mouseY) && button == 0) {
            if (!dragging) mc.player.playSound(SoundEvents.UI_BUTTON_CLICK, 1f, 1f);
            dragging = true;
        }
    }

    @Override
    public void mouseUp(int mouseX, int mouseY) {
        dragging = false;
    }

    @Override
    public void close() {
        dragging = false;
    }

    public static class Slider extends DoubleSetting {
        private final Setting<Double> setting;

        public Slider(Module module, Setting setting, int x, int y, int w, int h) {
            super(module, setting, x, y, w, h);
            this.setting = setting;
        }

        @Override
        protected void updateSlider(int mouseX) {
            final double diff = Math.min(w(), Math.max(0, mouseX - x()));
            final double min = setting.getMin();
            final double max = setting.getMax();
            sliderWidth = (w() - 6) * (setting.get() - min) / (max - min);

            if (dragging) {
                setting.setValue(diff == 0.0 ? setting.getMin() : Double.parseDouble(String.format("%." + setting.getInc() + "f", diff / w() * (max - min) + min).replace(",", ".")));
            }
        }
    }
}
