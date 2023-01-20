package me.eureka.harvest.systems.modules.combat.KillAura;

import me.eureka.harvest.Hack;
import me.eureka.harvest.events.event.Render3DEvent;
import me.eureka.harvest.events.event.TickEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.ic.util.Renderer3D;
import me.eureka.harvest.ic.util.RotationUtil;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.modules.combat.AutoCrystal.AutoCrystal;
import me.eureka.harvest.systems.modules.combat.Criticals.Criticals;
import me.eureka.harvest.systems.modules.render.HitMarker.HitMarker;
import me.eureka.harvest.systems.modules.render.LogoutSpot.PlayerCopyEntity;
import me.eureka.harvest.systems.utils.other.TimerUtils;
import com.google.common.eventbus.Subscribe;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.SwordItem;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Module.Info(name = "KillAura", category = Module.Category.Combat)
public class KillAura extends Module {
    public Setting<String> weapon = register("Weapon", List.of("Sword", "Axe", "Both", "Any"), "Any");
    public Setting<Boolean> smartDelay = register("SmartDelay", true);
    public Setting<Integer> actionDelay = register("ActionDelay", 500, 0, 1000);
    public Setting<Double> attackRange = register("AttackRange", 5, 0, 8, 1);
    public Setting<Boolean> thirtyTwo = register("32k", true);
    public Setting<Boolean> rotate = register("Rotate", false);

    public Setting<Boolean> players = register("Players", true);
    public Setting<Boolean> monsters = register("Monsters", true);
    public Setting<Boolean> animals = register("Animals", true);
    public Setting<Boolean> passive = register("Passive", false);
    public Setting<Boolean> villagers = register("Villagers", false);
    public Setting<Boolean> nametaged = register("Nametaged", false);

    public Setting<Boolean> pauseOnCA = super.register("PauseOnCA", false);

    public Setting<Boolean> render = super.register("Render", false);
    public Setting<Boolean> onHit = super.register("OnHit", false);

    public Entity target;
    private List<Entity> targets = new ArrayList<>();

    private final TimerUtils
            delayTimer = new TimerUtils(),
            renderTimer = new TimerUtils();

    @Override
    public void onActivate() {
        target = null;
        renderTimer.reset();
    }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        if (pauseOnCA.get() && AutoCrystal.INSTANCE.shouldPause()) return;

        targets = getEntities();
        if (targets.isEmpty()) return;

        if (!delayPassed()) return;
        if (!holdsWeapon()) return;

        target = targets.get(0);

        doAttack(target);
        setDisplayInfo(target.getName().getString());
    }

    private List<Entity> getEntities() {
        targets.clear();

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity)) continue;
            if (((LivingEntity) entity).isDead()) continue;

            if (mc.player.distanceTo(entity) <= attackRange.get()) targets.add(entity);
        }

        targets.removeIf(this::badTarget);
        targets.sort(Comparator.comparingDouble(this::distance));
        return targets;
    }

    private double distance(Entity entity) {
        return mc.player.distanceTo(entity);
    }

    private boolean badTarget(Entity entity) {
        if (entity == null) return true;
        if (entity.equals(mc.player) || entity.equals(mc.cameraEntity)) return true;
        if (entity instanceof PlayerCopyEntity) return true;

        if (!passive.get()) {
            if (entity instanceof EndermanEntity enderman && !enderman.isAngry()) return true;
            if (entity instanceof Tameable tameable && tameable.getOwnerUuid() != null && tameable.getOwnerUuid().equals(mc.player.getUuid()))
                return true;
            if (entity instanceof MobEntity mob && !mob.isAttacking() && !(entity instanceof PhantomEntity))
                return true;
        }

        if (entity instanceof PlayerEntity player) {
            if (Hack.friends().isFriend(player)) return true;
            if (player.isCreative() || player.isSpectator() || player.isDead()) return true;

            return !players.get();
        }
        if (entity instanceof HostileEntity) return !monsters.get();
        if (entity instanceof AnimalEntity) return !animals.get();
        if (entity instanceof VillagerEntity) return !villagers.get();

        return !nametaged.get() && entity.hasCustomName();
    }

    private boolean holdsWeapon() {
        switch (weapon.get()) {
            case "Sword" -> {
                return mc.player.getMainHandStack().getItem() instanceof SwordItem;
            }
            case "Axe" -> {
                return mc.player.getMainHandStack().getItem() instanceof AxeItem;
            }
            case "Both" -> {
                return mc.player.getMainHandStack().getItem() instanceof SwordItem || mc.player.getMainHandStack().getItem() instanceof AxeItem;
            }
            case "Any" -> {
                return true;
            }
        }

        return false;
    }

    private boolean delayPassed() {
        if (thirtyTwo.get() && is32k()) return true;
        if (smartDelay.get()) return mc.player.getAttackCooldownProgress(0.5f) >= 1;

        return delayTimer.passedMillis(actionDelay.get());
    }

    private boolean is32k() {
        int level = EnchantmentHelper.getLevel(Enchantments.SHARPNESS, mc.player.getMainHandStack());

        return level >= 20;
    }

    private void doAttack(Entity entity) {
        if (Criticals.instance.isActive()) Criticals.instance.doCritical();
        if (rotate.get()) RotationUtil.rotate(entity, false);
        AUtils.processAttack(entity);
        HitMarker.INSTANCE.post();

        delayTimer.reset();
        renderTimer.reset();
    }

    @Subscribe
    public void onRender(Render3DEvent event) {
        if (target == null) return;
        if (!render.get()) return;
        if (onHit.get() && renderTimer.passedTicks(5)) return;

        Renderer3D.get.drawFilledBox(event.matrix(), target, event.partial(), new Color(220, 0, 0, 140).getRGB());
    }

    public static KillAura instance;

    public KillAura() {
        instance = this;
    }
}
