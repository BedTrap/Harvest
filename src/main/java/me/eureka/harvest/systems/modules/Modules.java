package me.eureka.harvest.systems.modules;

import me.eureka.harvest.systems.modules.client.ClickGUI.ClickGUI;
import org.reflections.Reflections;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class Modules {
    private final List<Module> modules;

    public Modules() {
        modules = new ArrayList<>();

        Set<Class<? extends Module>> reflections = new Reflections("me.eureka.harvest.systems.modules").getSubTypesOf(Module.class);
        reflections.forEach(aClass -> {
            try {
                if (aClass.isAnnotationPresent(Module.Info.class)) modules.add(aClass.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        });

        modules.sort(Comparator.comparing(Module::getName));
    }

    public List<Module> get() {
        return modules;
    }

    public List<Module> get(Module.Category category) {
        List<Module> modules = new ArrayList<>();

        for (Module module : this.modules) {
            if (module.getCategory() == category) {
                modules.add(module);
            }
        }
        return modules;
    }

    public Module get(String name) {
        for (Module m : modules) {
            if (m.getName().equalsIgnoreCase(name))
                return m;
        }
        return null;
    }

    public Module get(Class<? extends Module> clazz) {
        for (Module module : modules) {
            if (module.getName().equalsIgnoreCase(clazz.getSimpleName())) return module;
        }

        return new ClickGUI();
    }
}
