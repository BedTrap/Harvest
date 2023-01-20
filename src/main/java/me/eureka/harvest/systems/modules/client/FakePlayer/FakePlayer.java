package me.eureka.harvest.systems.modules.client.FakePlayer;

import com.google.common.eventbus.Subscribe;
import com.mojang.authlib.GameProfile;
import me.eureka.harvest.events.event.EntityRemovedEvent;
import me.eureka.harvest.events.event.TickEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.modules.combat.KillAura.KillAura;
import me.eureka.harvest.systems.managers.DamageManager;
import me.eureka.harvest.systems.utils.other.TimerUtils;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Module.Info(name = "FakePlayer", category = Module.Category.Client)
public class FakePlayer extends Module {
    private final Setting<Boolean> allowPop = register("AllowPop", true);
    private final Setting<Boolean> autoStopREC = register("AutoStopREC", true);
    private final Setting<Integer> seconds = register("Seconds", 10, 5, 60);
    private final Setting<Integer> interpolateSteps = register("InterpolateSteps", 3, 1, 10);

    public PlayerEntity fakePlayer;
    private boolean shouldPop;
    private boolean shouldSwap;

    private final List<FortniteMoves> fortniteMoves = new ArrayList<>();

    private final TimerUtils recordTimer = new TimerUtils();

    private final TimerUtils hurtTimer = new TimerUtils();
    private final TimerUtils swapTimer = new TimerUtils();

    public static FakePlayer instance;

    public FakePlayer() {
        instance = this;
    }

    public Action action;
    public boolean startPlaying, startRecording;
    public int tick;

    @Override
    public void onActivate() {
        fakePlayer = new OtherClientPlayerEntity(mc.world, new GameProfile(UUID.fromString("c3924bf3-181b-4f91-a955-34314643cb85"), "tyrannus00"), mc.player.getPublicKey());

        fakePlayer.copyPositionAndRotation(mc.player);
        fakePlayer.setHealth(36);
        fakePlayer.getInventory().clone(mc.player.getInventory());

        mc.world.addEntity(-9999, fakePlayer);

        shouldPop = false;
        shouldSwap = false;

        hurtTimer.reset();
        swapTimer.reset();

        action = Action.Wait;
        startPlaying = false;
        startRecording = false;
        tick = 0;
    }

    @Subscribe
    public void onRecord(TickEvent.Post event) {
        if (action == null) {
            toggle();
            return;
        }

        if (action != Action.Stop && action != Action.Wait) setDisplayInfo(String.valueOf(tick));
        switch (action) {
            case Play -> {
                if (startPlaying) {
                    tick = 0;
                    startPlaying = false;
                }

                if (tick >= fortniteMoves.size()) {
                    startPlaying = true;
                    return;
                }

                FortniteMoves move = fortniteMoves.get(tick);
                fakePlayer.updateTrackedPositionAndAngles(move.pos.x, move.pos.y, move.pos.z, move.yaw, move.pitch, interpolateSteps.get(), false);
                fakePlayer.updateTrackedHeadRotation(move.yaw, interpolateSteps.get());
                tick++;
            }
            case Record -> {
                if (startRecording) {
                    fortniteMoves.clear();
                    recordTimer.reset();
                    startRecording = false;
                }

                fortniteMoves.add(new FortniteMoves(mc.player.getPos(), mc.player.getYaw(), mc.player.getPitch()));
                if (autoStopREC.get() && recordTimer.passedSec(seconds.get())) action = Action.Stop;
            }
            case Stop -> {
                info("FakePlayer", "Recording was stopped.");
                action = Action.Wait;
            }
            case Wait -> {

            }
        }
    }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        if (fakePlayer == null) {
            setToggled(false);
            return;
        }

        if (allowPop.get()) {
            if (shouldSwap && !swapTimer.passedTicks(3)) return;
            fakePlayer.setStackInHand(Hand.OFF_HAND, Items.TOTEM_OF_UNDYING.getDefaultStack());
            shouldSwap = false;
        } else {
            fakePlayer.setStackInHand(Hand.OFF_HAND, Items.AIR.getDefaultStack());
            return;
        }

        if ((mc.targetedEntity == fakePlayer && mc.options.attackKey.isPressed()) || (KillAura.instance.isActive() && KillAura.instance.target == fakePlayer && mc.player.handSwinging)) {
            if (!hurtTimer.passedTicks(10)) return;
            float damage = 0;

            if (canCrit()) {
                SoundEvent soundEvent = new SoundEvent(SoundEvents.ENTITY_PLAYER_ATTACK_CRIT.getId());

                mc.particleManager.addEmitter(fakePlayer, ParticleTypes.CRIT);
                mc.world.playSound(mc.player, fakePlayer.getBlockPos(), soundEvent, SoundCategory.PLAYERS, 1.0F, 1.0F);
                damage = 1;
            }

            damage += (float) DamageManager.getSwordDamage(fakePlayer) / 3;
            setHealth(damage);
        }

        fakePlayer.tick();
        if (!shouldPop) return;

        SoundEvent soundEvent = new SoundEvent(SoundEvents.ITEM_TOTEM_USE.getId());

        mc.particleManager.addEmitter(fakePlayer, ParticleTypes.TOTEM_OF_UNDYING, 30);
        mc.world.playSound(mc.player, fakePlayer.getBlockPos(), soundEvent, SoundCategory.PLAYERS, 1.0F, 1.0F);

        shouldPop = false;
        managePop(fakePlayer);
    }

    @Override
    public void onDeactivate() {
        if (fakePlayer != null) fakePlayer.remove(RemovalReason.DISCARDED);
    }

    @Subscribe
    private void onRemoved(EntityRemovedEvent event) {
        if (!(event.entity instanceof EndCrystalEntity)) return;

        if (!allowPop.get()) return;
        if (!hurtTimer.passedTicks(10)) return;

        float damage = (float) DamageManager.crystalDamage(fakePlayer, roundVec(event.entity));
        setHealth(damage);
    }

    private void setHealth(float damage) {
        float health = fakePlayer.getHealth() - damage;

        if (health < 0.5) {
            shouldPop = true;

            health = 20;
        }

        fakePlayer.setHealth(health);

        fakePlayer.animateDamage();
        hurtTimer.reset();
    }

    private boolean canCrit() {
        boolean flag = mc.player.getAttackCooldownProgress(0.5f) > 0.9;
        boolean flag2 = flag && mc.player.fallDistance > 0.0F && !mc.player.isOnGround() && !mc.player.isClimbing() && !mc.player.isSubmergedInWater() && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS);
        return flag2 && !mc.player.isSprinting();
    }

    private void managePop(PlayerEntity player) {
        if (player.getMainHandStack().getItem() == Items.TOTEM_OF_UNDYING) {
            player.setStackInHand(Hand.MAIN_HAND, Items.AIR.getDefaultStack());
            return;
        }

        if (player.getMainHandStack().getItem() == Items.TOTEM_OF_UNDYING && player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING)
            return;
        player.setStackInHand(Hand.OFF_HAND, Items.AIR.getDefaultStack());
        shouldSwap = true;
        swapTimer.reset();
    }

    public enum Action {
        Record, Stop, Play, Wait
    }

    public static Vec3d roundVec(Entity entity) {
        BlockPos blockPos = entity.getBlockPos().down();

        return new Vec3d(blockPos.getX() + 0.5, blockPos.getY() + 1, blockPos.getZ() + 0.5);
    }

    public static class FortniteMoves {
        private Vec3d pos;
        private float yaw, pitch;

        public FortniteMoves(Vec3d pos, float yaw, float pitch) {
            this.pos = pos;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }
}
