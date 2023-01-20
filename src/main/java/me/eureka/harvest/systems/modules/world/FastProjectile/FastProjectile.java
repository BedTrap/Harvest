package me.eureka.harvest.systems.modules.world.FastProjectile;

import me.eureka.harvest.events.event.PacketEvent;
import me.eureka.harvest.events.event.TickEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.systems.modules.Module;
import com.google.common.eventbus.Subscribe;
import net.minecraft.item.BowItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;

@Module.Info(name = "FastProjectile", category = Module.Category.World)
public class FastProjectile extends Module {
    public Setting<Integer> timeout = register("TimeOut", 3500, 1500, 7000);
    public Setting<Integer> spoofs = register("Spoofs", 5, 1, 25);
    public Setting<Boolean> bows = register("Bow", false);
    public Setting<Boolean> pearls = register("Pearl", true);
    public Setting<Boolean> bypass = register("Bypass", false);

    private long lastShootTime;

    @Override
    public void onActivate() {
        lastShootTime = System.currentTimeMillis();
    }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        if (System.currentTimeMillis() - lastShootTime >= timeout.get())
            setDisplayInfo("Charged");
        else setDisplayInfo("Charging");
    }

    @Subscribe
    public void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof PlayerActionC2SPacket packet) {
            if (packet.getAction() == PlayerActionC2SPacket.Action.RELEASE_USE_ITEM) {
                ItemStack handStack = mc.player.getStackInHand(Hand.MAIN_HAND);

                if (!handStack.isEmpty() && handStack.getItem() != null && handStack.getItem() instanceof BowItem && bows.get()) {
                    doSpoofs();
                }
            }

        } else if (event.packet instanceof PlayerInteractItemC2SPacket packet2) {
            if (packet2.getHand() == Hand.MAIN_HAND) {
                ItemStack handStack = mc.player.getStackInHand(Hand.MAIN_HAND);

                if (!handStack.isEmpty() && handStack.getItem() != null) {
                    if (handStack.getItem() instanceof EnderPearlItem && pearls.get()) {
                        doSpoofs();
                    }
                }
            }
        }
    }

    private void doSpoofs() {
        if (System.currentTimeMillis() - lastShootTime >= timeout.get()) {
            lastShootTime = System.currentTimeMillis();

            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));

            for (int i = 0; i < spoofs.get(); i++) {
                if (bypass.get()) {
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1e-10, mc.player.getZ(), false));
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() - 1e-10, mc.player.getZ(), true));
                } else {
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() - 1e-10, mc.player.getZ(), true));
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1e-10, mc.player.getZ(), false));
                }
            }
        }
    }
}
