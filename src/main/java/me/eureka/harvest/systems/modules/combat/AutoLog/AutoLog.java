package me.eureka.harvest.systems.modules.combat.AutoLog;

import me.eureka.harvest.Hack;
import me.eureka.harvest.events.event.TickEvent;
import me.eureka.harvest.ic.Friends;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.modules.render.Nametags.Nametags;
import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.DisconnectS2CPacket;
import net.minecraft.text.Text;

import java.util.List;

@Module.Info(name = "AutoLog", category = Module.Category.Combat)
public class AutoLog extends Module {
    public Setting<Boolean> logOnPlayer = register("LogOnPlayer", true);
    public Setting<Boolean> logOnHealth = register("LogOnHealth", false);
    public Setting<Integer> health = register("Health", 10, 1, 36);
    public Setting<Boolean> logOnTotems = register("LogOnTotems", false);
    public Setting<Integer> totems = register("Totems", 0, 0, 27);
    public Setting<Boolean> logOnPing = register("LogOnPing", false);
    public Setting<Integer> ping = register("Ping", 160, 5, 999);
    public Setting<Boolean> disableAfter = register("AutoToggle", false);
    public Setting<String> displayedInfo = register("DisplayedInfo", List.of("Health", "Totems", "Ping"), "Ping");

    @Subscribe
    public void onTick(TickEvent.Post event) {
        setInfo();

        if (logOnPlayer.get()) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player.isDead()) continue;
                if (mc.player == player) continue;
                if (Hack.friends().isFriend(player)) continue;

                mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(Text.literal("Your ping is higher than " + ping.get())));
                if (disableAfter.get()) toggle();
                break;
            }
        }

        if (logOnPing.get() && ping.get() <= mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()).getLatency()) {
            mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(Text.literal("Your ping is higher than " + ping.get())));
            if (disableAfter.get()) toggle();
        }

        if (logOnTotems.get() && totems.get() > mc.player.getInventory().count(Items.TOTEM_OF_UNDYING)) {
            mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(Text.literal("You have fewer totems than " + totems.get())));
            if (disableAfter.get()) toggle();
        }

        if (logOnHealth.get() && health.get() > mc.player.getHealth() + mc.player.getAbsorptionAmount()) {
            mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(Text.literal("Your health is less than  " + health.get())));
            if (disableAfter.get()) toggle();
        }
    }

    private void setInfo() {
        switch (displayedInfo.get()) {
            case "Health" -> setDisplayInfo((mc.player.getHealth() + mc.player.getAbsorptionAmount()) + "/" + health.get());
            case "Totems" -> setDisplayInfo(mc.player.getInventory().count(Items.TOTEM_OF_UNDYING) + "/" + totems.get());
            case "Ping" -> setDisplayInfo(Nametags.INSTANCE.getPing(mc.player, "bebra") + "/" + ping.get());
        }
    }
}