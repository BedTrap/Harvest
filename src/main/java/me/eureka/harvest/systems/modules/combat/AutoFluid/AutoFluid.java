package me.eureka.harvest.systems.modules.combat.AutoFluid;

import com.google.common.eventbus.Subscribe;
import me.eureka.harvest.Hack;
import me.eureka.harvest.events.event.Render3DEvent;
import me.eureka.harvest.events.event.TickEvent;
import me.eureka.harvest.ic.util.Draw3D;
import me.eureka.harvest.ic.util.Renderer3D;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.modules.client.Rotations.Rotations;
import me.eureka.harvest.systems.utils.advanced.BlockPosX;
import me.eureka.harvest.systems.utils.advanced.FindItemResult;
import me.eureka.harvest.systems.utils.advanced.InvUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

//@Module.Info(name = "AutoFluid", category = Module.Category.Combat)
public class AutoFluid extends Module {
    //private final Setting<String> mode = register("Mode", List.of("LavaCorner", "LavaCast"), "LavaCorner");

    private final BlockPosX[] SURROUND_CORNERS = new BlockPosX[4];

    private PlayerEntity closest_player;
    private BlockPosX lava_position;

    @Subscribe
    public void onTick(TickEvent.Post event) {
        generateCorners();
        findClosestPlayer();
        findClosestCorner();
        placeLava();
    }

    @Subscribe
    public void onRender(Render3DEvent event) {
        if (lava_position == null) return;

        Vec3d v = Renderer3D.getRenderPosition(lava_position.get());
        Box box = new Box(v.getX(), v.getY(), v.getZ(), v.getX() + 1, v.getY() + 1.0, v.getZ() + 1);
        Draw3D.drawOutlineBox(event, box, new Color(100, 100, 100, 200));
    }

    private void generateCorners() {
        SURROUND_CORNERS[0] = new BlockPosX(mc.player.getBlockPos().north().west());
        SURROUND_CORNERS[1] = new BlockPosX(mc.player.getBlockPos().south().east());
        SURROUND_CORNERS[2] = new BlockPosX(mc.player.getBlockPos().east().north());
        SURROUND_CORNERS[3] = new BlockPosX(mc.player.getBlockPos().west().south());
    }

    private void findClosestPlayer() {
        info("findClosestPlayer() ->");

        List<PlayerEntity> players = new ArrayList<>();
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity player) players.add(player);
        }

        players.stream()
                .filter(LivingEntity::isAlive)
                .filter((player) -> player != mc.player)
                .filter((player) -> !Hack.friends().isFriend(player))
                .min((o1, o2) -> Float.compare(mc.player.distanceTo(o1), mc.player.distanceTo(o2)))
                .ifPresentOrElse((player) -> {
                    info("Player found");
                    closest_player = player;
                }, () -> {
                    info("Player is null");
                    closest_player = null;
                });
    }

    private void findClosestCorner() {
        info("findClosestCorner() ->");
        if (closest_player != null) {
            Stream.of(SURROUND_CORNERS)
                    .min((o1, o2) -> Double.compare(o1.distance(closest_player), o2.distance(closest_player)))
                    .ifPresentOrElse((corner) -> {
                        info("Closest corner found");
                        lava_position = corner.down();
                    }, () -> {
                        info("Closest is null");
                        lava_position = null;
                    });
        }
    }

    private Pair<Boolean, Vec3d> canSeeCorners() {
        info("canSeeCorners() ->");

        if (lava_position == null) return new Pair<>(false, Vec3d.ZERO);
        Vec3d eye_pos = mc.player.getEyePos();

        int x = lava_position.x();
        int y = lava_position.y();
        int z = lava_position.z();

        List<Vec3d> closest_corners = Stream.of(
                        // Upper
                        new Vec3d(x + 1.0, y + 1.0, z + 1.0),
                        new Vec3d(x + 1.0, y + 1.0, z + 0.0),
                        new Vec3d(x + 0.0, y + 1.0, z + 0.0),
                        new Vec3d(x + 0.0, y + 1.0, z + 1.0),
                        // Lower
                        new Vec3d(x + 1.0, y + 0.0, z + 1.0),
                        new Vec3d(x + 1.0, y + 0.0, z + 0.0),
                        new Vec3d(x + 0.0, y + 0.0, z + 0.0),
                        new Vec3d(x + 0.0, y + 0.0, z + 1.0)
                ).sorted(Comparator.comparingDouble(v -> v.distanceTo(eye_pos)))
                .toList();

        for (Vec3d corner : closest_corners) {
            RaycastContext raycastContext = new RaycastContext(eye_pos, corner, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
            BlockHitResult result = mc.world.raycast(raycastContext);

            if (result.getPos().equals(corner)) {
                info("Found best Vec3d corner");
                return new Pair<>(true, corner);
            }
        }

        return new Pair<>(false, Vec3d.ZERO);
    }

    private void placeLava() {
        info("placeLava() ->");

        Pair<Boolean, Vec3d> corner_info = canSeeCorners();
        boolean corner_not_found = !corner_info.getLeft();

        if (corner_not_found) return;
        Vec3d best_corner = corner_info.getRight();

        BlockPosX block_pos = new BlockPosX(best_corner);
        if (block_pos.fluid()) return;

        FindItemResult bucket = InvUtils.findInHotbar(Items.LAVA_BUCKET);

        if (bucket.found()) {
            float[] yaw = new float[]{(float) Rotations.instance.getYaw(best_corner), mc.player.getYaw()};
            float[] pitch = new float[]{ (float) Rotations.instance.getPitch(best_corner), mc.player.getPitch()};

            mc.player.setYaw(yaw[0]);
            mc.player.setHeadYaw(yaw[0]);
            mc.player.setPitch(pitch[0]);

            InvUtils.swap(bucket.slot(), true);
            mc.interactionManager.interactItem(mc.player, bucket.getHand());
            InvUtils.swapBack();

            mc.player.setYaw(yaw[1]);
            mc.player.setHeadYaw(yaw[1]);
            mc.player.setPitch(pitch[1]);
        }
    }
}
