package me.eureka.harvest.systems.modules.combat.Quiver;

import me.eureka.harvest.events.event.TickEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.utils.advanced.FindItemResult;
import me.eureka.harvest.systems.utils.advanced.InvUtils;
import me.eureka.harvest.systems.utils.game.ChatUtils;
import me.eureka.harvest.systems.utils.other.TimerUtils;
import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.potion.PotionUtil;

import java.util.ArrayList;
import java.util.List;

@Module.Info(name = "Quiver", category = Module.Category.Combat)
public class Quiver extends Module {

    public Setting<Boolean> strength = register("Strength", true);
    public Setting<Boolean> speed = register("Speed", true);
    public Setting<Boolean> lowMode = register("LowMode", true);
    public Setting<Integer> delay = register("ReleaseInterval", 4, 0, 7);
    public Setting<Boolean> onlyOnGround = register("OnGround", true);
    public Setting<Boolean> checkEffects = register("IgnoreCurrent", false);
    public Setting<Boolean> silentBow = register("SilentBow", true);

    private final List<Integer> arrowSlots = new ArrayList<>();
    TimerUtils afterTimer = new TimerUtils();
    int interval;
    int prevBowSlot;

    @Override
    public void onActivate() {
        afterTimer.reset();
        interval = 0;
        prevBowSlot = -1;

        FindItemResult bow = InvUtils.find(Items.BOW);

        if (!bow.found()) {
            toggle();
            return;
        }

        if (silentBow.get() && !bow.isHotbar()) {
            prevBowSlot = bow.slot();
            InvUtils.move().from(bow.slot()).to(mc.player.getInventory().selectedSlot);
        } else if (!bow.isHotbar()) {
            ChatUtils.info(this.name, "No bow in inventory found.");
            toggle();
            return;
        }

        mc.options.useKey.setPressed(false);
        mc.interactionManager.stopUsingItem(mc.player);

        if (!silentBow.get()) InvUtils.swap(bow.slot(), true);

        arrowSlots.clear();

        List<StatusEffect> usedEffects = new ArrayList<>();

        for (int i = mc.player.getInventory().size(); i > 0; i--) {
            if (i == mc.player.getInventory().selectedSlot) continue;

            ItemStack item = mc.player.getInventory().getStack(i);

            if (item.getItem() != Items.TIPPED_ARROW) continue;

            List<StatusEffectInstance> effects = PotionUtil.getPotionEffects(item);

            if (effects.isEmpty()) continue;

            StatusEffect effect = effects.get(0).getEffectType();

            if ((strength.get() && effect == StatusEffects.STRENGTH || speed.get() && effect == StatusEffects.SPEED)
                    && !usedEffects.contains(effect)
                    && (!hasEffect(effect) || !checkEffects.get())) {
                usedEffects.add(effect);
                arrowSlots.add(i);
            }
        }
    }

    private boolean hasEffect(StatusEffect effect) {
        for (StatusEffectInstance statusEffect : mc.player.getStatusEffects()) {
            if (statusEffect.getEffectType() == effect) return true;
        }

        return false;
    }

    @Override
    public void onDeactivate() {
        if (lowMode.get()) mc.options.sneakKey.setPressed(false);
        if (silentBow.get() && prevBowSlot != -1)
            InvUtils.move().from(mc.player.getInventory().selectedSlot).to(prevBowSlot);
        else
            InvUtils.swapBack();
    }

    @Subscribe
    private void onTick(TickEvent.Pre event) {
        if (onlyOnGround.get() && !mc.player.isOnGround()) return;
        if (arrowSlots.isEmpty() || !InvUtils.findInHotbar(Items.BOW).isMainHand()) {
            if (afterTimer.passedSec(1)) toggle();
            return;
        }

        interval--;
        if (interval > 0) return;

        boolean charging = mc.options.useKey.isPressed();
        double charge = lowMode.get() ? 0.1 : 0.12;

        if (!charging) {
            InvUtils.move().from(arrowSlots.get(0)).to(9);
            mc.options.useKey.setPressed(true);
        } else {
            if (BowItem.getPullProgress(mc.player.getItemUseTime()) >= charge) {
                int targetSlot = arrowSlots.get(0);
                arrowSlots.remove(0);

                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(mc.player.getYaw(), -90, mc.player.isOnGround()));
                mc.options.useKey.setPressed(false);
                mc.interactionManager.stopUsingItem(mc.player);
                if (targetSlot != 9) InvUtils.move().from(9).to(targetSlot);
                if (arrowSlots.isEmpty() && lowMode.get()) mc.options.sneakKey.setPressed(true);
                interval = delay.get();
                afterTimer.reset();
            }
        }
    }
}
