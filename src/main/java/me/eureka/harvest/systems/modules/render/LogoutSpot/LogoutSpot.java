package me.eureka.harvest.systems.modules.render.LogoutSpot;

import me.eureka.harvest.Hack;
import me.eureka.harvest.events.event.PacketEvent;
import me.eureka.harvest.events.event.Render3DEvent;
import me.eureka.harvest.events.event.TickEvent;
import me.eureka.harvest.ic.Friends;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.ic.util.Renderer3D;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.modules.client.HUD.HUD;
import me.eureka.harvest.systems.modules.render.Nametags.Nametags;
import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

@Module.Info(name = "LogoutSpots", category = Module.Category.Render)
public class LogoutSpot extends Module {
    public Setting<String> render = register("Render", List.of("Box", "Player"), "Box");
    public Setting<String> formatting = register("Formatting", List.of("Italic", "Bold", "Both", "None"), "None");
    public Setting<Double> scale = register("Scale", 1.1, 0.5, 2, 2);
    public Setting<Boolean> background = register("Background", true);

    public Setting<Boolean> pops = register("Pops", true);
    public Setting<Boolean> friends = register("Friends", false);
    public Setting<Boolean> notify = register("Notify", true);
    public Setting<Boolean> feedback = register("Feedback", true);
    public Setting<Boolean> clear = register("Clear", false);

    private final Spot spots = new Spot();

    @Override
    public void onActivate() {
        if (spots.hashMap.isEmpty()) return;

        for (Map.Entry<String, PlayerCopyEntity> map : spots.hashMap.entrySet()) {
            PlayerCopyEntity copyEntity = map.getValue();

            copyEntity.spawn();
        }
    }

    @Override
    public void onDeactivate() {
        if (spots.hashMap.isEmpty()) return;

        for (Map.Entry<String, PlayerCopyEntity> map : spots.hashMap.entrySet()) {
            PlayerCopyEntity copyEntity = map.getValue();

            copyEntity.despawn();
        }
    }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        if (clear.get()) {
            if (!spots.hashMap.isEmpty()) {
                for (Map.Entry<String, PlayerCopyEntity> map : spots.hashMap.entrySet()) {
                    PlayerCopyEntity copyEntity = map.getValue();

                    copyEntity.despawn();
                }
            }
            spots.getSpots().clear();
            spots.hashMap.clear();

            clear.setValue(false);
        }
    }

    @Subscribe
    public void onPacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof PlayerListS2CPacket packet)) return;
        Nametags nametags = Nametags.INSTANCE;

        switch (packet.getAction()) {
            case REMOVE_PLAYER -> packet.getEntries().forEach(entry -> {
                if (!spots.valid(entry)) return;
                if (!spots.online(entry)) return;

                PlayerEntity player = spots.player(entry);
                if (friends.get() && Hack.friends().isFriend(player)) return;

                String name = player.getGameProfile().getName();
                float hp = spots.entryHealth(entry);
                int pops = nametags.getPops(player);

                spots.add(name, player, hp, pops);
            });
            case ADD_PLAYER -> packet.getEntries().forEach(entry -> {
                String name = entry.getProfile().getName();
                if (!spots.wasLogged(name)) return;

                if (notify.get()) info(this.name, name + " logged back.", feedback.get());
                spots.remove(spots.getSpot(name));
            });
        }
    }

    @Subscribe
    public void onRender(Render3DEvent event) {
        if (spots.getSpots().isEmpty()) return;
        Nametags nametags = Nametags.INSTANCE;

        for (Spot spot : spots.getSpots()) {
            PlayerEntity player = spot.getPlayer();
            Vec3d vec3d = spot.getTextPos();

            if (render.get("Box")) {
                Renderer3D.get.drawFilledBox(event.matrix(), player, 0, HUD.INSTANCE.colorType(140).getRGB());
            }

            // Nametags
            double size = Math.max(2 * (mc.gameRenderer.getCamera().getPos().distanceTo(player.getPos()) / 20), 1);
            DecimalFormat df = new DecimalFormat("##.#");
            String pos = df.format(player.getX()) + " " + df.format(player.getY()) + " " + df.format(spot.getPlayer().getZ());

            Renderer3D.drawText(
                    Text.of(Formatting.GRAY + nametags.shortForm(formatting.get()) + "[LOG] " + spot.getName() +
                            (pops.get() ? nametags.formattingByPops(spot.getPops(), nametags.shortForm(formatting.get())) + Formatting.GRAY : "") +
                            nametags.shortForm(formatting.get()) + " XYZ " + pos), vec3d.x, vec3d.y, vec3d.z, scale.get() * size, background.get());
        }
    }

    public static LogoutSpot INSTANCE;

    public LogoutSpot() {
        INSTANCE = this;
    }
}
