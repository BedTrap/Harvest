package me.eureka.harvest.systems.utils.advanced;

import me.eureka.harvest.Hack;
import me.eureka.harvest.ic.Friends;
import me.eureka.harvest.systems.modules.render.LogoutSpot.PlayerCopyEntity;
import me.eureka.harvest.ic.util.Wrapper;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class TargetUtils {
    private static final List<Entity> ENTITIES = new ArrayList<>();

    public static Entity get(Predicate<Entity> isGood, SortPriority sortPriority) {
        ENTITIES.clear();
        getList(ENTITIES, isGood, sortPriority, 1);
        if (!ENTITIES.isEmpty()) {
            return ENTITIES.get(0);
        }

        return null;
    }

    public static void getList(List<Entity> targetList, Predicate<Entity> isGood, SortPriority sortPriority, int maxCount) {
        targetList.clear();

        for (Entity entity : Wrapper.mc.world.getEntities()) {
            if (entity != null && isGood.test(entity)) targetList.add(entity);
        }

        targetList.sort((e1, e2) -> sort(e1, e2, sortPriority));
        targetList.removeIf(entity -> targetList.indexOf(entity) > maxCount -1);
    }

    public static PlayerEntity getPlayerTarget(double range, SortPriority priority) {
        if (Wrapper.mc.world == null) return null;
        return (PlayerEntity) get(entity -> {
            if (entity instanceof PlayerCopyEntity) return false;
            if (!(entity instanceof PlayerEntity) || entity == Wrapper.mc.player) return false;
            if (((PlayerEntity) entity).isDead() || ((PlayerEntity) entity).getHealth() <= 0) return false;
            if (Wrapper.mc.player.distanceTo(entity) > range) return false;
            if (Hack.friends().isFriend((PlayerEntity) entity)) return false;
            return !((PlayerEntity) entity).isCreative() || entity instanceof OtherClientPlayerEntity;
        }, priority);
    }

    public static boolean isBadTarget(PlayerEntity target, double range) {
        if (target == null) return true;
        return Wrapper.mc.player.distanceTo(target) > range || !target.isAlive() || target.isDead() || target.getHealth() <= 0;
    }

    private static int sort(Entity e1, Entity e2, SortPriority priority) {
        return switch (priority) {
            case LowestDistance -> Double.compare(e1.distanceTo(Wrapper.mc.player), e2.distanceTo(Wrapper.mc.player));
            case HighestDistance -> invertSort(Double.compare(e1.distanceTo(Wrapper.mc.player), e2.distanceTo(Wrapper.mc.player)));
            case LowestHealth -> sortHealth(e1, e2);
            case HighestHealth -> invertSort(sortHealth(e1, e2));
        };
    }

    private static int sortHealth(Entity e1, Entity e2) {
        boolean e1l = e1 instanceof LivingEntity;
        boolean e2l = e2 instanceof LivingEntity;

        if (!e1l && !e2l) return 0;
        else if (e1l && !e2l) return 1;
        else if (!e1l) return -1;

        return Float.compare(((LivingEntity) e1).getHealth(), ((LivingEntity) e2).getHealth());
    }
    private static int invertSort(int sort) {
        if (sort == 0) return 0;
        return sort > 0 ? -1 : 1;
    }

    public enum SortPriority {
        LowestDistance, HighestDistance, LowestHealth, HighestHealth
    }
}
