package me.eureka.harvest.systems.modules.player.NoRotate;

import me.eureka.harvest.events.event.PacketEvent;
import me.eureka.harvest.mixins.PlayerPositionLookS2CPacketAccessor;
import me.eureka.harvest.systems.modules.Module;
import com.google.common.eventbus.Subscribe;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

@Module.Info(name = "NoRotate", category = Module.Category.Player)
public class NoRotate extends Module {

    @Subscribe
    private void onPacket(PacketEvent.Receive event) {
        if (event.packet instanceof PlayerPositionLookS2CPacket && mc.player != null) {
            ((PlayerPositionLookS2CPacketAccessor) event.packet).setPitch(mc.player.getPitch());
            ((PlayerPositionLookS2CPacketAccessor) event.packet).setYaw(mc.player.getYaw());
        }
    }
}
