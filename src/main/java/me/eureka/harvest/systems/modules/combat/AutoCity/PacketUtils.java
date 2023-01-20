package me.eureka.harvest.systems.modules.combat.AutoCity;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.ic.util.Wrapper;
import me.eureka.harvest.mixininterface.IClientPlayerInteractionManager;
import me.eureka.harvest.systems.utils.advanced.BlockPosX;
import me.eureka.harvest.systems.utils.advanced.FindItemResult;
import me.eureka.harvest.systems.utils.advanced.InvUtils;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.tag.FluidTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;

import static me.eureka.harvest.ic.util.Wrapper.mc;

public class PacketUtils {
    private double progress;
    public Stage stage;

    public PacketUtils() {
        this.progress = 0;
        this.stage = Stage.NONE;
    }

    public double progress() {
        return this.progress;
    }
    public void reset() {
        this.progress = 0.0;
        this.stage = Stage.NONE;
    }

    public void mine(BlockPosX bp, Runnable onStart, Runnable onEnd, Setting<String> swap , Setting<Boolean> inventory) {
        FindItemResult itemResult = inventory.get() ? InvUtils.findFastestTool(bp.state()) : InvUtils.findFastestToolInHotbar(bp.state());
        int slot = itemResult.found() ? itemResult.slot() : mc.player.getInventory().selectedSlot;

        if (bp.air()) return;
        switch (stage) {
            case NONE -> {
                onStart.run();
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, bp, bp.closestDirection()));
                this.stage = Stage.START;
            }
            case START -> {
                if (progress >= 1.0) {
                    this.stage = Stage.STOP;
                } else progress += getBreakDelta(slot, bp.state());
            }
            case STOP -> {
                onEnd.run();
                if (!inventory.get()) {
                    int prevSlot = mc.player.getInventory().selectedSlot;
                    if (swap.get("Normal") || swap.get("Silent")) {
                        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(itemResult.slot()));
                        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, bp, bp.closestDirection()));
                        if (swap.get("Silent")) mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(prevSlot));
                    }

                    if (swap.get("Move")) {
                        move(itemResult.slot(), () -> mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, bp, bp.closestDirection())));
                    }
                } else move(itemResult.slot(), () -> mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, bp, bp.closestDirection())));

                this.progress = 0;
                this.stage = Stage.NONE;
            }
        }
    }

    public boolean canBreak(BlockPosX bp, double range) {
        if (bp.unbreakable()) return false;
        if (bp.fluid()) return false;
        return !(bp.distance() > range);
    }

    private double getBreakDelta(int slot, BlockState state) {
        float hardness = state.getHardness(null, null);

        if (hardness == -1) return 0;
        else {
            return getBlockBreakingSpeed(slot, state) / hardness / (!state.isToolRequired() || mc.player.getInventory().main.get(slot).isSuitableFor(state) ? 30 : 100);
        }
    }

    private static double getBlockBreakingSpeed(int slot, BlockState block) {
        double speed = mc.player.getInventory().main.get(slot).getMiningSpeedMultiplier(block);

        if (speed > 1) {
            ItemStack tool = mc.player.getInventory().getStack(slot);

            int efficiency = EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, tool);

            if (efficiency > 0 && !tool.isEmpty()) speed += efficiency * efficiency + 1;
        }

        if (StatusEffectUtil.hasHaste(mc.player)) {
            speed *= 1 + (StatusEffectUtil.getHasteAmplifier(mc.player) + 1) * 0.2F;
        }

        if (mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            float k = switch (mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) {
                case 0 -> 0.3F;
                case 1 -> 0.09F;
                case 2 -> 0.0027F;
                default -> 8.1E-4F;
            };

            speed *= k;
        }

        if (mc.player.isSubmergedIn(FluidTags.WATER) && !EnchantmentHelper.hasAquaAffinity(mc.player)) {
            speed /= 5.0F;
        }

        if (!mc.player.isOnGround()) {
            speed /= 5.0F;
        }

        return speed;
    }

    private void move(int slot, Runnable runnable) {
        if (slot == -1 || slot == 44) {
            runnable.run();
            return;
        }

        move(mc.player.getInventory().selectedSlot, slot);
        runnable.run();
        move(mc.player.getInventory().selectedSlot, slot);
    }

    private void move(int from, int to) {
        ScreenHandler handler = mc.player.currentScreenHandler;

        Int2ObjectArrayMap<ItemStack> stack = new Int2ObjectArrayMap<>();
        stack.put(to, handler.getSlot(to).getStack());

        mc.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(handler.syncId, handler.getRevision(), PlayerInventory.MAIN_SIZE + from, to, SlotActionType.SWAP, handler.getCursorStack().copy(), stack));
    }

    public enum Stage {
       NONE, START, STOP
    }
}