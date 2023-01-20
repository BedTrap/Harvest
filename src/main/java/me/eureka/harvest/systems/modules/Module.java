package me.eureka.harvest.systems.modules;

import me.eureka.harvest.Hack;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.systems.utils.game.ChatUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Formatting;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

public class Module extends ChatUtils {
    public static MinecraftClient mc = MinecraftClient.getInstance();

    public String name;
    public String displayInfo;
    public int bind;
    public boolean toggled;
    public boolean drawn;
    public double arrayAnimation = -1;
    public Category category;
    public String description;
    
    private boolean silent = false;

    public Module() {
        Info info = getClass().getAnnotation(Info.class);
        this.bind = info.bind();
        this.name = info.name();
        this.displayInfo = "";
        this.category = info.category();
        this.toggled = false;
        this.description = "";
        this.drawn = info.drawn();
    }

    public void onActivate() {

    }

    public void onDeactivate() {

    }

    public String getName() {
        return name;
    }

    public String getDisplayInfo() {
        return displayInfo;
    }

    public void setDisplayInfo(String info) {
        info = info.replace(",", Formatting.GRAY + "," + Formatting.WHITE);
        this.displayInfo = Formatting.GRAY + " [" + Formatting.WHITE + info + Formatting.GRAY + "]";
    }

    public Category getCategory() {
        return category;
    }

    public int getBind() {
        return bind;
    }

    public void setBind(int key) {
        this.bind = key;
    }

    public boolean isBounded() {
        return this.bind != -1;
    }

    public boolean isActive() {
        return toggled;
    }

    public void setToggled(boolean toggled) {
        this.toggled = toggled;
        if (toggled) enable();
        else if (!toggled) disable();
    }

    public void toggle() {
        this.toggled = !this.toggled;
        if (toggled) enable();
        else if (!toggled) disable();
    }

    public void toggle(boolean silent) {
        this.silent = silent;

        this.toggled = !this.toggled;
        if (toggled) enable();
        else if (!toggled) disable();
    }

    public boolean isDrawn() {
        return drawn;
    }

    public void setDrawn(boolean drawn) {
        this.drawn = drawn;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void enable() {
        Hack.event().register(this);
        arrayAnimation = -1;

        if (cantUpdate()) return;
        onActivate();

        infoModule(name, true);
    }

    public void disable() {
        Hack.event().unregister(this);

        if (cantUpdate()) return;
        onDeactivate();

        if (!this.silent) infoModule(name, false);
        this.silent = false;
    }

    protected Setting<Integer> register(final String name, final int value, final int min, final int max) {
        final Setting<Integer> s = new Setting<>(name, this, getCategory(), value, min, max, 0, true);
        Hack.settings().addSetting(s);
        return s;
    }

    protected Setting<Double> register(final String name, final double value, final double min, final double max, final int inc) {
        final Setting<Double> s = new Setting<>(name, this, getCategory(), value, min, max, inc);
        Hack.settings().addSetting(s);
        return s;
    }

    protected Setting<Boolean> register(final String name, final boolean value) {
        final Setting<Boolean> s = new Setting<>(name, this, getCategory(), value);
        Hack.settings().addSetting(s);
        return s;
    }

    protected Setting<String> register(final String name, final List<String> modes, final String value) {
        final Setting<String> s = new Setting<>(name, this, getCategory(), modes, value);
        Hack.settings().addSetting(s);
        return s;
    }

    public boolean cantUpdate() {
        return mc.player == null && mc.world == null;
    }


    public enum Category {
        Combat, Player, Miscellaneous, Movement, Render, World, Client
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Info {
        String name();
        Module.Category category();
        boolean drawn() default true;
        int bind() default -1;
    }
}
