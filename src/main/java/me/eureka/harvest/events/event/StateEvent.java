package me.eureka.harvest.events.event;

import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.systems.modules.Module;

public class StateEvent {
    public static class Mode {
        public Module module;
        public String mode;

        public Mode(Module module, String mode) {
            this.module = module;
            this.mode = mode;
        }
    }

    public static class Button {
        public Module module;
        public Setting<Boolean> setting;

        public Button(Module module, Setting<Boolean> setting) {
            this.module = module;
            this.setting = setting;
        }
    }

    public static class Number {
        public Module module;
        public Setting<Integer> setting;

        public Number(Module module, Setting<Integer> setting) {
            this.module = module;
            this.setting = setting;
        }
    }
}
