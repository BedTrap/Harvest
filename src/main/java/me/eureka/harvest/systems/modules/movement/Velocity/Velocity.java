package me.eureka.harvest.systems.modules.movement.Velocity;

import me.eureka.harvest.events.event.PacketEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.mixins.IEntityVelocityUpdateS2CPacketAccessor;
import me.eureka.harvest.mixins.IExplosionS2CPacketAccessor;
import me.eureka.harvest.systems.modules.Module;
import com.google.common.eventbus.Subscribe;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;

@Module.Info(name = "Velocity", category = Module.Category.Movement)
public class Velocity extends Module {
    public Setting<Boolean> knockBack = register("Knockback", true);
    public Setting<Boolean> push = register("Push", true);

    @Subscribe
    private void onPacket(PacketEvent.Receive event) {
        if (mc.player == null) return;

        if (knockBack.get() && event.packet instanceof EntityVelocityUpdateS2CPacket packet && ((EntityVelocityUpdateS2CPacket) event.packet).getId() == mc.player.getId()) {
            double velX = (packet.getVelocityX() / 8000d - mc.player.getVelocity().x) * 0;
            double velY = (packet.getVelocityY() / 8000d - mc.player.getVelocity().y) * 0;
            double velZ = (packet.getVelocityZ() / 8000d - mc.player.getVelocity().z) * 0;
            ((IEntityVelocityUpdateS2CPacketAccessor) packet).setX((int) (velX * 8000 + mc.player.getVelocity().x * 8000));
            ((IEntityVelocityUpdateS2CPacketAccessor) packet).setY((int) (velY * 8000 + mc.player.getVelocity().y * 8000));
            ((IEntityVelocityUpdateS2CPacketAccessor) packet).setZ((int) (velZ * 8000 + mc.player.getVelocity().z * 8000));
        } else if (event.packet instanceof ExplosionS2CPacket) {
            ExplosionS2CPacket velocity = (ExplosionS2CPacket) event.packet;
            ((IExplosionS2CPacketAccessor) velocity).setPlayerVelocityX(velocity.getPlayerVelocityX() * 0);
            ((IExplosionS2CPacketAccessor) velocity).setPlayerVelocityY(velocity.getPlayerVelocityY() * 0);
            ((IExplosionS2CPacketAccessor) velocity).setPlayerVelocityZ(velocity.getPlayerVelocityZ() * 0);
        }
    }

    public static Velocity get;

    public Velocity() {
        get = this;
    }
}

