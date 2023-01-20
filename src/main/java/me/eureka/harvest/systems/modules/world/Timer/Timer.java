package me.eureka.harvest.systems.modules.world.Timer;

import me.eureka.harvest.events.event.TickEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.systems.modules.Module;
import com.google.common.eventbus.Subscribe;

@Module.Info(name = "Timer", category = Module.Category.World)
public class Timer extends Module {
    public static Timer get;
    public Setting<Double> factor = register("Factor", 1, 0.1, 5, 1);

    public Timer() {
        get = this;
    }

    public static float override = 1;

    @Subscribe
    public void onTick(TickEvent.Post event){
        get = this;
    }

    public float getMultiplier() {
        if (override != 1) return override;
        if (isActive()) return factor.get().floatValue();
        return 1;
    }

    public void timerOverride(float override) {
        this.override = override;
    }

}
