package me.eureka.harvest.systems.modules.render.Nametags;

import me.eureka.harvest.Hack;
import me.eureka.harvest.events.event.GameJoinedEvent;
import me.eureka.harvest.events.event.PacketEvent;
import me.eureka.harvest.events.event.Render3DEvent;
import me.eureka.harvest.ic.Friends;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.ic.util.Renderer3D;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.modules.render.LogoutSpot.PlayerCopyEntity;
import com.google.common.eventbus.Subscribe;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.text.DecimalFormat;
import java.util.List;
import java.util.UUID;

import static me.eureka.harvest.systems.modules.miscellaneous.Announcer.AUtils.*;

@Module.Info(name = "Nametags", category = Module.Category.Render)
public class Nametags extends Module {
    public Setting<Boolean> ping = register("Ping", true);
    public Setting<Boolean> health = register("Health", true);
    public Setting<Boolean> pops = register("Pops", true);
    public Setting<Double> scale = register("Scale", 1.1, 0.5, 2, 2);
    public Setting<String> formatting = register("Formatting", List.of("Italic", "Bold", "Both", "None"), "None");
    public Setting<Boolean> background = register("Background", true);

    private final Object2IntMap<UUID> totemPopMap = new Object2IntOpenHashMap<>();
    int popped;

    @Override
    public void onActivate() {
        totemPopMap.clear();
        popped = 0;
    }

    @Subscribe
    public void onRender(Render3DEvent event) {
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerCopyEntity) continue;

            Vec3d rPos = entity.getPos().subtract(Renderer3D.getInterpolationOffset(entity)).add(0, entity.getHeight() + 0.50, 0);
            double size = Math.max(2 * (mc.gameRenderer.getCamera().getPos().distanceTo(entity.getPos()) / 20), 1);

            if (entity instanceof PlayerEntity player) {
                if (player == mc.player || player.isDead()) continue;
                if (isBot(player)) continue;

                Renderer3D.drawText(Text.of(" " + getPlayerInfo(player) + " "), rPos.x, rPos.y, rPos.z, scale.get() * size, background.get());
            }
        }
    }

    @Subscribe
    private void onPop(PacketEvent.Receive event) {
        if (!pops.get()) return;
        if (!(event.packet instanceof EntityStatusS2CPacket p)) return;
        if (p.getStatus() != 35) return;
        Entity entity = p.getEntity(mc.world);
        if (!(entity instanceof PlayerEntity)) return;
        if ((entity.equals(mc.player))) return;

        synchronized (totemPopMap) {
            popped = totemPopMap.getOrDefault(entity.getUuid(), 0);
            totemPopMap.put(entity.getUuid(), ++popped);
        }
    }

    @Subscribe
    public void onPacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof PlayerListS2CPacket packet)) return;

        switch (packet.getAction()) {
            case UPDATE_GAME_MODE, REMOVE_PLAYER -> packet.getEntries().forEach(entry -> {
                UUID uuid = entry.getProfile().getId();

                if (isOnline(uuid)) {
                    if (getModeFromUUID(uuid).equals(entry.getGameMode())) {
                        synchronized (totemPopMap) {
                            if (!totemPopMap.containsKey(uuid)) return;

                            totemPopMap.removeInt(uuid);
                        }
                    }
                }
            });
        }
    }

    @Subscribe
    public void onJoin(GameJoinedEvent event) {
        totemPopMap.clear();
        popped = 0;
    }

    private String getPlayerInfo(PlayerEntity player) {
        StringBuilder sb = new StringBuilder();

        sb.append(formattingByName(player, shortForm(formatting.get())) + (ping.get() ? " " + getPing(player, shortForm(formatting.get())) + "ms" : ""));
        sb.append(health.get() ? formattingByHealth(player, shortForm(formatting.get())) : "");
        sb.append(pops.get() ? formattingByPops(getPops(player), shortForm(formatting.get())) : "");

        return sb.toString();
    }

    public int getPops(PlayerEntity p) {
        if (!totemPopMap.containsKey(p.getUuid())) return 0;
        return totemPopMap.getOrDefault(p.getUuid(), 0);
    }

    public String getPing(PlayerEntity player, String formatting) {
        if (mc.getNetworkHandler() == null) return formatting + "0";

        PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
        if (playerListEntry == null) return formatting + "0";
        return formatting + playerListEntry.getLatency();
    }

    public String formattingByHealth(PlayerEntity player, String formatting) {
        DecimalFormat df = new DecimalFormat("##");
        double hp = Math.round(player.getHealth() + player.getAbsorptionAmount());
        String health = df.format(Math.round(hp));

        if (hp >= 19) return Formatting.GREEN + " " + formatting +  health;
        if (hp >= 13 && hp <= 18) return Formatting.YELLOW + " " + formatting + health;
        if (hp >= 8 && hp <= 12) return Formatting.GOLD + " " + formatting + health;
        if (hp >= 6 && hp <= 7) return Formatting.RED + " " + formatting + health;
        if (hp <= 5) return Formatting.DARK_RED + " " + formatting + health;

        return Formatting.GREEN + formatting + " unexpected";
    }

    public String shortForm(String formatting) {
        return switch (formatting) {
            case "Italic" -> "" + Formatting.ITALIC;
            case "Bold" -> "" + Formatting.BOLD;
            case "Both" -> "" + Formatting.ITALIC + Formatting.BOLD;
            default -> "";
        };
    }

    public String formattingByName(PlayerEntity player, String formatting) {
        if (player.isInSneakingPose()) return Formatting.GOLD + formatting + player.getEntityName();
        if (Hack.friends().isFriend(player.getEntityName()))
            return Formatting.AQUA + formatting + player.getName().getString();

        return Formatting.WHITE + formatting + player.getEntityName();
    }

    public String formattingByPops(int pops, String formatting) {
        return switch (pops) {
            case 0 -> "";
            case 1 -> Formatting.GREEN + formatting + " -" + pops;
            case 2 -> Formatting.DARK_GREEN + formatting + " -" + pops;
            case 3 -> Formatting.YELLOW + formatting + " -" + pops;
            case 4 -> Formatting.GOLD + formatting + " -" + pops;
            case 5 -> Formatting.RED + formatting + " -" + pops;
            default -> Formatting.DARK_RED + formatting + " -" + pops;
        };
    }

    private boolean isBot(PlayerEntity player) {
        for (String bot : List.of("xViiRuS", "chpn", "CIT")) {
            if (player.getGameProfile().getName().startsWith(bot)) return true;
        }

        return false;
    }

    public static Nametags INSTANCE;

    public Nametags() {
        INSTANCE = this;
    }
}
