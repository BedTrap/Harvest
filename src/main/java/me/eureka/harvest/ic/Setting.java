package me.eureka.harvest.ic;

import me.eureka.harvest.systems.modules.Module;

import java.util.List;

public class Setting<T> {
    private final String name;
    private final Module parent;
    private final Module.Category category;
    private final Type type;

    private T value, max, min;
    private int inc;
    private List<String> modes;

    private String description;

    public Setting(final String name, final Module parent, final Module.Category category, T value) {
        this.name = name;
        this.parent = parent;
        this.type = Type.Boolean;
        this.category = category;
        this.value = value;
    }

    public Setting(final String name, final Module parent, final Module.Category category, T value, T min, T max, int inc, boolean integer) {
        this.name = name;
        this.parent = parent;
        this.type = Type.Integer;
        this.category = category;
        this.value = value;
        this.min = min;
        this.max = max;
        this.inc = inc;
    }

    public Setting(final String name, final Module parent, final Module.Category category, T value, T min, T max, int inc) {
        this.name = name;
        this.parent = parent;
        this.type = Type.Double;
        this.category = category;
        this.value = value;
        this.min = min;
        this.max = max;
        this.inc = inc;
    }

    public Setting(String name, Module parent, Module.Category category, T value, boolean page) {
        this.name = name;
        this.parent = parent;
        this.type = Type.Page;
        this.category = category;
        this.value = value;
    }

    public Setting(final String name, final Module parent, final Module.Category category, List<String> modes, T value) {
        this.name = name;
        this.parent = parent;
        this.type = Type.Mode;
        this.category = category;
        this.value = value;
        this.modes = modes;
    }

    public String getName() {
        return this.name;
    }

    public Module getParent() {
        return this.parent;
    }

    public Type getType() {
        return this.type;
    }

    public Module.Category getCategory() {
        return this.category;
    }

    public T get() {
        return value;
    }
    public boolean get(String mode) {
        return value.equals(mode);
    }

    public void setValue(T value) {
        this.value = value;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    public String getDescription() {
        return this.description;
    }

    public T getMin() {
        return min;
    }

    public T getMax() {
        return max;
    }

    public int getInc() {
        return inc;
    }

    public List<String> getModes() {
        return modes;
    }

    public enum Type {
        Integer, Double, Boolean, Page, Mode
    }
}
