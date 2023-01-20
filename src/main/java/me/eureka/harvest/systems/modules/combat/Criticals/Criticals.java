package me.eureka.harvest.systems.modules.combat.Criticals;

import me.eureka.harvest.events.event.PacketEvent;
import me.eureka.harvest.systems.modules.Module;
import com.google.common.eventbus.Subscribe;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

@Module.Info(name = "Criticals", category = Module.Category.Combat)
public class Criticals extends Module {
    @Subscribe
    public void sendPacket(PacketEvent.Send event) {
        if (event.packet instanceof PlayerInteractEntityC2SPacket packet) {
            if (!(getInteractType(packet) == InteractType.ATTACK && getEntity(packet) instanceof LivingEntity)) return;
            if (getEntity(packet) instanceof EndCrystalEntity) return;

            doCritical();
        }
    }

    public void doCritical() {
        if (mc.player.isInLava() || mc.player.isTouchingWater()) return;

        double posX = mc.player.getX();
        double posY = mc.player.getY();
        double posZ = mc.player.getZ();
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(posX, posY + 0.0633, posZ, false));
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(posX, posY, posZ, false));
    }

    private Entity getEntity(PlayerInteractEntityC2SPacket packet) {
        PacketByteBuf packetBuf = new PacketByteBuf(Unpooled.buffer());
        packet.write(packetBuf);

        return mc.world.getEntityById(packetBuf.readVarInt());
    }

    private InteractType getInteractType(PlayerInteractEntityC2SPacket packet) {
        PacketByteBuf packetBuf = new PacketByteBuf(Unpooled.buffer());
        packet.write(packetBuf);

        packetBuf.readVarInt();
        return packetBuf.readEnumConstant(InteractType.class);
    }

    public enum InteractType {
        INTERACT,
        ATTACK,
        INTERACT_AT
    }

    public static Criticals instance;

    public Criticals() {
        instance = this;
    }
}
