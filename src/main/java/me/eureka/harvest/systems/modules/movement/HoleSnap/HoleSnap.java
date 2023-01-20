package me.eureka.harvest.systems.modules.movement.HoleSnap;

import com.google.common.eventbus.Subscribe;
import me.eureka.harvest.events.event.MoveEvent;
import me.eureka.harvest.events.event.Render3DEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.ic.util.Renderer3D;
import me.eureka.harvest.mixininterface.IVec3d;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.modules.client.HUD.HUD;
import me.eureka.harvest.systems.utils.advanced.BlockPosX;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

@Module.Info(name = "HoleSnap", category = Module.Category.Movement)
public class HoleSnap extends Module {
    public Setting<Integer> range = register("Range", 3, 1, 6);
    public Setting<Boolean> render = register("Render", false);

    private boolean snapped;
    private BlockPosX ignored = null;

    @Override
    public void onActivate() {
        snapped = false;
        ignored = null;
    }

    @Subscribe
    public void onMove(MoveEvent.Player event) {
        if (ignored != null) {
            if (ignored.x() != mc.player.getBlockX() && ignored.z() != mc.player.getBlockZ()) ignored = null;
        }

        for (int i = 0; i < range.get(); i++) {
            Vec3d vec3d = new Vec3d(mc.player.getX(), (mc.player.getBlockPos().getY() + 0.5) - i, mc.player.getZ());
            Box box = new Box(vec3d.x - 0.3, vec3d.y - 0.3, vec3d.z - 0.3, vec3d.x + 0.3, vec3d.y, vec3d.z + 0.3);

            if (isHole(vec3d) && canSnap(box) && validSpace(vec3d)) {
                ((IVec3d) event.movement).setXZ(0, 0);
                if (mc.player.isOnGround()) {
                    ignored = new BlockPosX(mc.player.getBlockPos());
                } else snapped = true;
            }
        }
    }

    @Subscribe
    public void onRender(Render3DEvent event) {
        if (!render.get()) return;

        for (int i = 1; i < range.get(); i++) {
            Vec3d vec3d = new Vec3d(mc.player.getX(), (mc.player.getBlockPos().getY() + 0.5) - i, mc.player.getZ());
            Box box = new Box(vec3d.x - 0.3, vec3d.y - 0.3, vec3d.z - 0.3, vec3d.x + 0.3, vec3d.y, vec3d.z + 0.3);

            if (isHole(vec3d) && canSnap(box) && validSpace(vec3d)) {
                Vec3d v = Renderer3D.getRenderPosition(box.getCenter());
                Box bb = new Box(v.x - 0.3, v.y - v.x - 0.3, v.z - 0.3, v.x + 0.3, v.y + v.x - 0.3, v.z + 0.3);
                Renderer3D.get.drawBoxWithOutline(event.matrix(), bb, HUD.INSTANCE.colorType(140));
            }
        }
    }

    public boolean snapped() {
        return this.snapped;
    }

    private boolean validSpace(Vec3d vec3d) {
        int size = (int) (mc.player.getBlockY() - vec3d.y);

        for (int i = 0; i <= size; i++) {
            BlockPosX bp = new BlockPosX(vec3d).up(i);
            if (!bp.air()) return false;
        }

        return true;
    }

    private boolean isHole(Vec3d vec3d) {
        BlockPosX bp = new BlockPosX(vec3d);
        if (ignored != null) {
            if (ignored.equals(bp)) return false;
        }

        for (Direction direction : Direction.values()) {
            if (direction == Direction.UP) continue;
            if (!bp.offset(direction).blastRes()) return false;
        }

        return true;
    }

    public boolean canSnap(Box box) {
        for (int x = (int) Math.floor(box.minX); x < Math.ceil(box.maxX); x++) {
            for (int y = (int) Math.floor(box.minY); y < Math.ceil(box.maxY); y++) {
                for (int z = (int) Math.floor(box.minZ); z < Math.ceil(box.maxZ); z++) {
                    BlockState state = mc.world.getBlockState(new BlockPos(x, y, z));

                    if (!state.isAir()) return false;
                }
            }
        }

        return true;
    }

    public static HoleSnap INSTANCE;

    public HoleSnap() {
        INSTANCE = this;
    }
}
