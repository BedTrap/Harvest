package me.eureka.harvest.systems.utils.anticheat;

import me.eureka.harvest.mixins.MinecraftClientAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import static me.eureka.harvest.ic.util.Wrapper.mc;

// When I was decompiling VH addon (https://github.com/BedTrap/Venomhack420-src) I saw class like AntiCheat
// And then I made this, this is very helpful, but not finished
public class AntiCheat {
    public static void processClick(Click click, Entity entity) {
        switch (click) {
            case M1 -> {
                // This code block is underrated by a lot of mc coders
                // But I know this thing will be used in the most future mc clients
                // P.S.: Simulates left mouse click aka vanilla hit, so everything will be under vanilla condition
                // P.S. 2: I killed Iggy with thing, when I had troubles with normal attack using interactionManager or packet attack :trolleybus:
                HitResult prevResult = mc.crosshairTarget;
                mc.crosshairTarget = new EntityHitResult(entity, closestVec3d(entity, Range.EYE_POS));
                leftClick();
                mc.crosshairTarget = prevResult;
            }
            case M2 -> {
                // TODO: 02.12.2022 add interaction
            }
        }
    }

    public static boolean outOfRange(Entity entity, double range) {
        return mc.player.getEyePos().distanceTo(closestVec3d(entity, Range.EYE_POS)) > range;
    }

    private static void leftClick() {
        mc.options.attackKey.setPressed(true);
        ((MinecraftClientAccessor) mc).leftClick();
        mc.options.attackKey.setPressed(false);
    }

    private static Vec3d closestVec3d(Entity entity, Range range) {
        Vec3d pos = switch (range) {
            case EYE_POS -> mc.player.getEyePos();
            case PLAYER_POS -> mc.player.getPos();
        };

        Box box = entity.getBoundingBox();

        double x = MathHelper.clamp(pos.getX(), box.minX, box.maxX);
        double y = MathHelper.clamp(pos.getY(), box.minY, box.maxY);
        double z = MathHelper.clamp(pos.getZ(), box.minZ, box.maxZ);

        return new Vec3d(x, y, z);
    }


    public static void canSee(Box box, Consumer<Vec3d> consumer) {
        List<Vec3d> points = collectVec3d(box, 6 /*vanilla*/);

        for (Vec3d point : points) {
            if (canSee(point)) {
                consumer.accept(point);
                break;
            }
        }
    }

    /**
     * @param vec3d the point which you want to check
     * @return true there's no blocks between player eyes and "vec3d"
     */
    public static boolean canSee(Vec3d vec3d) {
        Vec3d eye_pos = mc.player.getEyePos();

        RaycastContext raycastContext = new RaycastContext(eye_pos, vec3d, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
        BlockHitResult result = mc.world.raycast(raycastContext);

        return result.getPos().equals(vec3d);
    }

    /**
     * @param box which box you want to check
     * @param range range between player eyes and box point
     * @return list of a sorted box points
     */
    private static List<Vec3d> collectVec3d(Box box, double range) {
        List<Vec3d> list = new ArrayList<>();
        Vec3d eye_pos = mc.player.getEyePos();

        for (int x = (int) Math.floor(box.minX); x < Math.ceil(box.maxX); x++) {
            for (int y = (int) Math.floor(box.minY); y < Math.ceil(box.maxY); y++) {
                for (int z = (int) Math.floor(box.minZ); z < Math.ceil(box.maxZ); z++) {
                    Vec3d point = new Vec3d(x, y, z);

                    if (eye_pos.distanceTo(point) > range) continue;
                    list.add(point);
                }
            }
        }

        list.sort(Comparator.comparingDouble(eye_pos::distanceTo));
        return list;
    }

    public static boolean isWithinRange(Entity entity, Range range, double value) {
        Vec3d entity_pos = entity.getPos();
        Vec3d player_pos = switch (range) {
            case EYE_POS -> mc.player.getEyePos();
            case PLAYER_POS -> mc.player.getPos();
        };

        return !(player_pos.distanceTo(entity_pos) > value);
    }

    public static boolean canKill(PlayerEntity player_entity) {
        if (player_entity.isDead()) return false;
        if (player_entity.isCreative() || player_entity.isSpectator()) return false;
        // TODO: 16.12.2022 hurt time

        return player_entity.isAttackable() && player_entity.canHit();
    }
}
