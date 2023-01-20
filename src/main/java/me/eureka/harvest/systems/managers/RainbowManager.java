package me.eureka.harvest.systems.managers;

import com.google.common.eventbus.Subscribe;
import me.eureka.harvest.events.event.MoveEvent;
import me.eureka.harvest.systems.modules.client.Prefix.Prefix;
import me.eureka.harvest.systems.utils.other.TimerUtils;
import net.minecraft.util.math.MathHelper;

import java.awt.*;

public class RainbowManager {
    private TimerUtils timer = new TimerUtils();
    private int rainbowHue;

    public Color getColorViaHue(float hue) {
        float v = 100.0F;
        if (hue > 270.0F) {
            hue = 0.0F;
        }
        return Color.getHSBColor(hue / 270.0F, 1, v / 100.0F);
    }

    public Color getColorViaHue(float hue, float s) {
        float v = 100.0f;
        if (hue > 270.0F) {
            hue = 0.0F;
        }
        return Color.getHSBColor(hue / 270.0F, s, v / 100.0F);
    }

    public Color getColorViaHue(float hue, float s, float b) {
        if (hue > 270.0F) {
            hue = 0.0F;
        }
        return Color.getHSBColor(hue / 270.0F, s, b);
    }

    public Color getColor(int color) {
        float alpha = (color >> 24 & 0xFF) / 255.0F;
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        return new Color(red, green, blue, alpha);
    }

    public int redGreenShift(float value) {
        value = MathHelper.clamp(value, 0, 1);
        return new Color(1 - value, value, 0).getRGB();
    }

    public int getRainbowColor() {
        return getColorViaHue(rainbowHue).getRGB();
    }

    public int getRainbowColor(int delta) {
        int rainbow = this.rainbowHue + delta;
        if (rainbow > 270) {
            rainbow -= 270;
        }

        return getColorViaHue(rainbow).getRGB();
    }

    @Subscribe
    public void onRender(MoveEvent.Player event) {
        if (!timer.passedMillis(Prefix.INSTANCE.speed.get())) return;

        this.rainbowHue++;
        if (rainbowHue > 270) {
            rainbowHue -= 270;
        }

        timer.reset();
    }

    public static RainbowManager INSTANCE;

    public RainbowManager() {
        INSTANCE = this;
        rainbowHue = 0;
    }
}