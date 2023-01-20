package me.eureka.harvest.systems.modules.render.BlockSelection;

import com.google.common.eventbus.Subscribe;
import me.eureka.harvest.events.event.Render3DEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.ic.util.Draw3D;
import me.eureka.harvest.ic.util.Renderer3D;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.modules.client.HUD.HUD;
import me.eureka.harvest.systems.utils.advanced.BlockPosX;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

import java.awt.*;
import java.util.List;

@Module.Info(name = "BlockSelection", category = Module.Category.Render)
public class BlockSelection extends Module {
    public Setting<String> render = super.register("Render", List.of("Default", "Smooth", "Shape"), "Default");
    public Setting<Integer> factor = super.register("Factor", 5, 1, 10);
    public Setting<String> shapeMode = register("ShapeMode", List.of("Line", "Fill", "Both"), "Both");
    public Setting<Integer> red = register("Red", 140, 0, 255);
    public Setting<Integer> green = register("Green", 140, 0, 255);
    public Setting<Integer> blue = register("Blue", 140, 0, 255);
    public Setting<Integer> alpha = register("Alpha", 140, 0, 255);
    private Box renderBox;

    @Override
    public void onActivate() {
        renderBox = null;
    }

    @Subscribe
    public void onRender(Render3DEvent event) {
        if (mc.crosshairTarget == null || !(mc.crosshairTarget instanceof BlockHitResult hitResult)) return;
        BlockPosX bp = new BlockPosX(hitResult.getBlockPos());

        Vec3d rendering = Renderer3D.getRenderPosition(bp.get());
        VoxelShape shape = bp.state().getOutlineShape(mc.world, bp.get());

        if (shape.isEmpty()) return;
        Box box = shape.getBoundingBox().offset(rendering);
        Color color = new Color(red.get(), green.get(), blue.get(), alpha.get());

        switch (render.get()) {
            case "Default" -> {
                switch (shapeMode.get()) {
                    case "Line" -> Draw3D.drawOutlineBox(event, box, color);
                    case "Fill" -> Draw3D.drawFilledBox(event, box, color);
                    case "Both" -> Draw3D.drawBox(event, box, color);
                }
            }
            case "Smooth" -> {
                if (renderBox == null) renderBox = box;

                double minX = (box.minX - renderBox.minX) / factor.get();
                double minY = (box.minY - renderBox.minY) / factor.get();
                double minZ = (box.minZ - renderBox.minZ) / factor.get();

                double maxX = (box.maxX - renderBox.maxX) / factor.get();
                double maxY = (box.maxY - renderBox.maxY) / factor.get();
                double maxZ = (box.maxZ - renderBox.maxZ) / factor.get();

                renderBox = new Box(renderBox.minX + minX, renderBox.minY + minY, renderBox.minZ + minZ, renderBox.maxX + maxX, renderBox.maxY + maxY, renderBox.maxZ + maxZ);
                switch (shapeMode.get()) {
                    case "Line" -> Draw3D.drawOutlineBox(event, box, color);
                    case "Fill" -> Draw3D.drawFilledBox(event, box, color);
                    case "Both" -> Draw3D.drawBox(event, box, color);
                }
            }
            case "Shape" -> shape.forEachBox((x1, y1, z1, x2, y2, z2) -> {
                renderBox = new Box(x1, y1, z1, x2, y2, z2).offset(rendering);

                switch (shapeMode.get()) {
                    case "Line" -> Draw3D.drawOutlineBox(event, box, color);
                    case "Fill" -> Draw3D.drawFilledBox(event, box, color);
                    case "Both" -> Draw3D.drawBox(event, box, color);
                }
            });
        }
    }
}
