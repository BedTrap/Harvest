package me.eureka.harvest.systems.modules.miscellaneous.AutoRespawn;

import me.eureka.harvest.events.event.TickEvent;
import me.eureka.harvest.systems.modules.Module;
import com.google.common.eventbus.Subscribe;

@Module.Info(name = "AutoRespawn", category = Module.Category.Miscellaneous)
public class AutoRespawn extends Module {

    @Subscribe
    private void onTick(TickEvent.Post event) {
        if (mc.player.isDead()) {
            mc.player.requestRespawn();
            mc.setScreen(null);
        }
    }
}

