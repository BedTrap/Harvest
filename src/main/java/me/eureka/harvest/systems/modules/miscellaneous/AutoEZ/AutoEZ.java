package me.eureka.harvest.systems.modules.miscellaneous.AutoEZ;

import com.google.common.eventbus.Subscribe;
import me.eureka.harvest.Hack;
import me.eureka.harvest.events.event.PacketEvent;
import me.eureka.harvest.events.event.TickEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.systems.modules.Module;
import net.fabricmc.mappings.model.CommentEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

import java.util.List;
import java.util.Random;

@Module.Info(name = "AutoEZ", category = Module.Category.Miscellaneous)
public class AutoEZ extends Module {
    @Subscribe
    public void onPacket(PacketEvent.Receive event) {
        if (event.packet instanceof EntityStatusS2CPacket packet) {
            if (packet.getStatus() != 3) return;

            Entity entity = packet.getEntity(mc.world);
            if (!(entity instanceof PlayerEntity player)) return;

            mc.player.sendChatMessage(message(name), null);
        }
    }

    public String message(String name) {
        List<String> messages = List.of(
                "Owned with ease",
                name + " should get Harvest somehow",
                "Seems you have died to me when i was afk",
                "Imagine using Meteor Client",
                "Imagine using VenomSkid",
                "Imagine using Skidnana+",
                "Imagine using sk1deor",
                "Imagine not using Meteor Reject");

        Random random = new Random();
        return messages.get(random.nextInt(messages.size()));
    }
}
