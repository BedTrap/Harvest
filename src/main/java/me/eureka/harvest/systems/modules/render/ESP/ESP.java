package me.eureka.harvest.systems.modules.render.ESP;

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
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.List;

@Module.Info(name = "ESP", category = Module.Category.Render)
public class ESP extends Module {
    public Setting<String> mode = register("Mode", List.of("Line", "Fill", "Both"), "Fill");
    public Setting<Boolean> players = register("Players", true);
    public Setting<Boolean> friends = register("Friends", true);
    public Setting<Boolean> animals = register("Animals", false);
    public Setting<Boolean> monsters = register("Monsters", true);
    public Setting<Boolean> items = register("Items", true);
    public Setting<Boolean> others = register("Others", false);
    public Setting<Integer> alpha = register("Alpha", 140, 0, 255);
    public Setting<Boolean> debug = register("Debug", false);

    @Subscribe
    public void onRender(Render3DEvent event) {
        int i = 0;

        for (Entity entity : mc.world.getEntities()) {
            if (debug.get() && entity instanceof EndCrystalEntity crystal) {
                Vec3d rPos = crystal.getPos();

                Renderer3D.drawText(Text.of(String.valueOf(mc.player.canSee(entity))), rPos.x, rPos.y + crystal.getStandingEyeHeight(), rPos.z, 0.8, false);
            }

            if (players.get() && entity instanceof PlayerEntity player && player != mc.player && !Hack.friends().isFriend(player)) {
                render(event, player, new Color(220, 0, 0), alpha.get());
                i++;
            }
            if (friends.get() && entity instanceof PlayerEntity friend && friend != mc.player && Hack.friends().isFriend(friend)) {
                render(event, friend, Color.CYAN, alpha.get());
                i++;
            }
            if (animals.get() && entity instanceof AnimalEntity animal) {
                render(event, animal, new Color(0, 220, 0), alpha.get());
                i++;
            }
            if (monsters.get() && entity instanceof Monster monster) {
                render(event, (Entity) monster, new Color(220, 0, 0), alpha.get());
                i++;
            }
            if (items.get() && entity instanceof ItemEntity item) {
                int a = entity.age * 10;
                if (a > alpha.get() - 1) a = alpha.get();
                render(event, item, new Color(ClickGUI.INSTANCE.r.get(), ClickGUI.INSTANCE.g.get(), ClickGUI.INSTANCE.b.get()), a);
                i++;
            }
            if (others.get() && !(entity instanceof PlayerEntity) && !(entity instanceof ItemEntity) && !(entity instanceof Monster) && !(entity instanceof AnimalEntity)) {
                render(event, entity, new Color(ClickGUI.INSTANCE.r.get(), ClickGUI.INSTANCE.g.get(), ClickGUI.INSTANCE.b.get()), alpha.get());
                i++;
            }
        }
        setDisplayInfo(String.valueOf(i));
    }

    private void render(Render3DEvent event, Entity entity, Color color, int alpha) {
        if (mode.get("Line") || mode.get("Both")) Renderer3D.get.drawEntityBox(event.matrix(), entity, event.partial(), new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha).getRGB());
        if (mode.get("Fill") || mode.get("Both")) Renderer3D.get.drawFilledBox(event.matrix(), entity, event.partial(), new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha).getRGB());
    }



    public static ESP INSTANCE;

    public ESP() {
        INSTANCE = this;
    }
}
