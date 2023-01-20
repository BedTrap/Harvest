package me.eureka.harvest.systems.modules.render.HoleESP;

import me.eureka.harvest.events.event.Render3DEvent;
import me.eureka.harvest.ic.util.Renderer3D;
import me.eureka.harvest.systems.modules.client.ClickGUI.ClickGUI;
import me.eureka.harvest.systems.modules.client.HUD.HUD;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.awt.*;

public class Hole {
    public BlockPos blockPos;
    public Hole.HoleType type;

    public Hole(BlockPos blockPos, Hole.HoleType holeType) {
        this.blockPos = blockPos;
        this.type = holeType;
    }

    public Color safeColor(int alpha) {
        if (ClickGUI.INSTANCE.g.get() + ClickGUI.INSTANCE.b.get() <= 99) return new Color(0, 120, 0, alpha);
        return HUD.INSTANCE.colorType(alpha);
    }

    public Color getColor(int alpha) {
        return switch (this.type) {
            case Unsafe -> new Color(120, 0, 0, alpha);
            case Safe -> safeColor(alpha);
        };
    }

    public void render(Render3DEvent event, double renderHeight, int alpha) {
        Vec3d vec3d = Renderer3D.getRenderPosition(blockPos.getX(), blockPos.getY(), blockPos.getZ());
        Box box = new Box(vec3d.getX(), vec3d.getY(), vec3d.getZ(), vec3d.getX() + 1, vec3d.getY() + renderHeight, vec3d.getZ() + 1);

        switch (HoleESP.INSTANCE.mode.get()) {
            case "Line" -> Renderer3D.get.drawOutlineBox(event.matrix(), box, getColor(alpha).getRGB());
            case "Fill" -> Renderer3D.get.drawFilledBox(event.matrix(), box, getColor(alpha).getRGB());
            case "Both" -> {
                Renderer3D.get.drawBox(event.matrix(), box, getColor(alpha).getRGB());
                Renderer3D.get.drawOutlineBox(event.matrix(), box, getColor(alpha).getRGB());
            }
        }
    }

    public enum HoleType {
        Safe,
        Unsafe,
    }
}