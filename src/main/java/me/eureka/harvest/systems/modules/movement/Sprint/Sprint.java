package me.eureka.harvest.systems.modules.movement.Sprint;

import me.eureka.harvest.events.event.MoveEvent;
import me.eureka.harvest.events.event.SprintEvent;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.ic.Setting;
import com.google.common.eventbus.Subscribe;

import java.util.Arrays;

@Module.Info(name = "Sprint", category = Module.Category.Movement)
public class Sprint extends Module {
    Setting<String> mode = register("Mode", Arrays.asList("Normal", "Rage"), "Normal");

    @Subscribe
    public void onMove(MoveEvent.Player event) {
        setDisplayInfo(mode.get());
        if (mc.player.input.movementForward + mc.player.input.movementSideways != 0)
            mc.player.setSprinting(true);
    }

    @Subscribe
    public void onShit(SprintEvent event) {
        if (mode.get("Rage") && !mc.player.isSubmergedInWater() && !mc.player.isInLava() && !mc.player.input.jumping)
            event.setSprinting(false).setCancelled(true);
    }
}
