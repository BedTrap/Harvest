package me.eureka.harvest.systems.modules.render.Tracers;

import me.eureka.harvest.Hack;
import me.eureka.harvest.events.event.Render3DEvent;
import me.eureka.harvest.ic.Friends;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.ic.util.Renderer3D;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.modules.client.ClickGUI.ClickGUI;
import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.awt.*;

@Module.Info(name = "Tracers", category = Module.Category.Render)
public class Tracers extends Module {
    public Setting<Boolean> players = register("Players", true);
    public Setting<Boolean> friends = register("Friends", true);
    public Setting<Boolean> animals = register("Animals", false);
    public Setting<Boolean> monsters = register("Monsters", true);
    public Setting<Boolean> items = register("Items", true);
    public Setting<Boolean> others = register("Others", false);

    @Subscribe
    public void onRender(Render3DEvent event) {
        int i = 0;

        for (Entity entity : mc.world.getEntities()) {
            if (players.get() && entity instanceof PlayerEntity player && player != mc.player && !Hack.friends().isFriend(player)) {
                renderTracer(event, player, new Color(220, 0, 0));
                i++;
            }
            if (friends.get() && entity instanceof PlayerEntity friend && friend != mc.player && Hack.friends().isFriend(friend)) {
                renderTracer(event, friend, Color.CYAN);
                i++;
            }
            if (animals.get() && entity instanceof AnimalEntity animal) {
                renderTracer(event, animal, new Color(0, 220, 0));
                i++;
            }
            if (monsters.get() && entity instanceof Monster monster) {
                renderTracer(event, (Entity) monster, new Color(220, 0, 0));
                i++;
            }
            if (items.get() && entity instanceof ItemEntity item) {
                int a = entity.age * 10;
                if (a > 254) a = 255;
                renderTracer(event, item, new Color(ClickGUI.INSTANCE.r.get(), ClickGUI.INSTANCE.g.get(), ClickGUI.INSTANCE.b.get()));
                i++;
            }
            if (others.get() && !(entity instanceof PlayerEntity) && !(entity instanceof ItemEntity) && !(entity instanceof Monster) && !(entity instanceof AnimalEntity)) {
                renderTracer(event, entity, new Color(ClickGUI.INSTANCE.r.get(), ClickGUI.INSTANCE.g.get(), ClickGUI.INSTANCE.b.get()));
                i++;
            }
        }

        setDisplayInfo(String.valueOf(i));
    }

    private void renderTracer(Render3DEvent event, Entity entity, Color color) {
        if (mc.crosshairTarget == null) return;
        Vec3d vec3d = Renderer3D.getRenderPosition(entity.getPos());
        Vec3d player = Renderer3D.getRenderPosition(mc.crosshairTarget.getPos());

        Renderer3D.drawLine(event.matrix(), (float) vec3d.x, (float) vec3d.y, (float) vec3d.z, (float) player.x, (float) player.y, (float) player.z, color.getRGB());
    }

    public static Tracers INSTANCE;

    public Tracers() {
        INSTANCE = this;
    }
}
