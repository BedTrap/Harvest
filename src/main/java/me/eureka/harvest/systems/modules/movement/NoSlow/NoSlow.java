package me.eureka.harvest.systems.modules.movement.NoSlow;

import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.systems.modules.Module;

@Module.Info(name = "NoSlow", category = Module.Category.Movement)
public class NoSlow extends Module {

    public static NoSlow get;
    public Setting<Boolean> airStrict = register("AirStrict", true);

    public NoSlow() {
        get = this;
    }

}
