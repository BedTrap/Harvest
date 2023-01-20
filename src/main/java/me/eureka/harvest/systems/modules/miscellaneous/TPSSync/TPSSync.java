package me.eureka.harvest.systems.modules.miscellaneous.TPSSync;

import me.eureka.harvest.events.event.TickEvent;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.modules.world.Timer.Timer;
import me.eureka.harvest.systems.modules.client.HUD.HUD;
import com.google.common.eventbus.Subscribe;

@Module.Info(name = "TPSSync", category = Module.Category.Miscellaneous)
public class TPSSync extends Module {

    @Override
    public void onDeactivate() {
        Timer.get.timerOverride(1.0F);
    }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        float tps = HUD.INSTANCE.getTickRate();

        Timer.get.timerOverride((tps >= 1 ? tps : 1) / 20);
    }
}
