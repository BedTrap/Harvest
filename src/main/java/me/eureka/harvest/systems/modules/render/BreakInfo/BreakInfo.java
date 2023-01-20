package me.eureka.harvest.systems.modules.render.BreakInfo;

import me.eureka.harvest.events.event.Render3DEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.ic.util.Renderer3D;
import me.eureka.harvest.mixins.WorldRendererAccessor;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.modules.combat.AutoCrystal.AutoCrystal;
import me.eureka.harvest.systems.modules.client.HUD.HUD;
import com.google.common.eventbus.Subscribe;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.BlockBreakingInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

import java.util.List;
import java.util.Map;

@Module.Info(name = "BreakInfo", category = Module.Category.Render)
public class BreakInfo extends Module {
    public Setting<String> mode = register("Mode", List.of("Text", "Box", "Both"), "Text");
    public Setting<Double> scale = register("Scale", 1, 0.01, 1.0, 2);

    @Subscribe
    public void onRender(Render3DEvent event) {
        Map<Integer, BlockBreakingInfo> blocks = ((WorldRendererAccessor) mc.worldRenderer).getBlockBreakingInfos();

        blocks.values().forEach(info -> {
            BlockPos pos = info.getPos();
            int stage = info.getStage();

            BlockState state = mc.world.getBlockState(pos);
            VoxelShape shape = state.getOutlineShape(mc.world, pos);
            if (shape.isEmpty()) return;
            double shrinkFactor = (9 - (stage + 1)) / 9d;
            double progress = 1d - shrinkFactor;

            boolean compatibility = AutoCrystal.INSTANCE.isActive() && AutoCrystal.INSTANCE.renderPos != null && AutoCrystal.INSTANCE.renderPos.equals(pos);
            Entity entity = mc.world.getEntityById(info.getActorId());
            PlayerEntity player = entity == null ? null : (PlayerEntity) entity;

            if (!mode.get("Box") && player != null) {
                Vec3d rPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5);
                Renderer3D.drawText(Text.of(((double) Math.round(progress * 100) / 100) + "%"), rPos.x, compatibility ? rPos.y - 0.15  : (rPos.y - scale.get() / 6.666), rPos.z, scale.get(), false);
                Renderer3D.drawText(Text.of(player.getGameProfile().getName()), rPos.x, compatibility ? rPos.y - 0.30 : ((rPos.y - (scale.get() / 9.9)) - scale.get() / 3.333), rPos.z, scale.get(), false);
            }

            if (!mode.get("Text")) {
                double max = ((double) Math.round(progress * 100) / 100);
                double min = 1 - max;

                Vec3d vec3d = Renderer3D.getRenderPosition(pos.getX(), pos.getY(), pos.getZ());
                Box box = new Box(vec3d.x + min, vec3d.y + min, vec3d.z + min, vec3d.x + max, vec3d.y + max, vec3d.z + max);
                Renderer3D.get.drawOutlineBox(event.matrix(), box, HUD.INSTANCE.color());
            }
        });
    }
}
