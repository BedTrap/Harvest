package me.eureka.harvest;

import me.eureka.harvest.gui.Gui;
import me.eureka.harvest.ic.Configs;
import me.eureka.harvest.ic.Friends;
import me.eureka.harvest.ic.Settings;
import me.eureka.harvest.systems.commands.Commands;
import me.eureka.harvest.systems.managers.RainbowManager;
import me.eureka.harvest.systems.modules.Modules;
import me.eureka.harvest.systems.managers.DamageManager;
import com.google.common.eventbus.EventBus;
import net.fabricmc.api.ModInitializer;

public class Hack implements ModInitializer {
    public static final String NAME = "Harvest";
    public static final String VERSION = "3.4";

    private static final EventBus EVENT_BUS = new EventBus();

    private static Commands COMMANDS;
    private static Settings SETTINGS;
    private static Modules MODULES;
    private static Friends FRIENDS;
    private static Gui GUI;

    @Override
    public void onInitialize() {
        System.out.println("Initializing " + NAME + "...");
        long ms = System.currentTimeMillis();

        COMMANDS = new Commands();
        SETTINGS = new Settings();
        MODULES = new Modules();
        FRIENDS = new Friends();
        GUI = new Gui();

        Configs CONFIGS = new Configs();
        CONFIGS.load();

        // Managers
        event().register(new DamageManager());
        event().register(new RainbowManager());

        System.out.println("Harvest started in " + (System.currentTimeMillis() - ms) + " ms.");
        Runtime.getRuntime().addShutdownHook(new Configs());
    }

    public static EventBus event() {
        return EVENT_BUS;
    }
    public static Modules modules() {
        return MODULES;
    }
    public static Commands commands() {
        return COMMANDS;
    }
    public static Friends friends() {
        return FRIENDS;
    }
    public static Settings settings() {
        return SETTINGS;
    }
    public static Gui gui() {
        return GUI;
    }

    public static Hack INSTANCE;

    public Hack() {
        INSTANCE = this;
    }
}
