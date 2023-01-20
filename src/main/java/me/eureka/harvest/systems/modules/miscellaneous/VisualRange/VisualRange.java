package me.eureka.harvest.systems.modules.miscellaneous.VisualRange;

import me.eureka.harvest.Hack;
import me.eureka.harvest.events.event.EntityAddedEvent;
import me.eureka.harvest.events.event.EntityRemovedEvent;
import me.eureka.harvest.ic.Friends;
import me.eureka.harvest.systems.modules.Module;
import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Formatting;

@Module.Info(name = "VisualRange", category = Module.Category.Miscellaneous)
public class VisualRange extends Module {
    @Subscribe
    private void onAdded(EntityAddedEvent event) {
        if (!(event.entity instanceof PlayerEntity player) || mc.player == player) return;

        info(name, getMessage(player, true));
    }

    @Subscribe
    private void onRemoved(EntityRemovedEvent event) {
        if (!(event.entity instanceof PlayerEntity player) || mc.player == player) return;

        info(name, getMessage(player, false));
    }

    private String getMessage(PlayerEntity player, boolean added) {
        String name = player.getGameProfile().getName();

        if (added) return Hack.friends().isFriend(player) ? "Epic dude " + Formatting.GREEN + name + Formatting.GRAY + " is here." : "I see an some nn called " + Formatting.RED + name + Formatting.GRAY + ".";
        return Hack.friends().isFriend(player) ? Formatting.GREEN + name + Formatting.GRAY + " is gone." : Formatting.RED + name + Formatting.GRAY + " got scared.";
    }
}
