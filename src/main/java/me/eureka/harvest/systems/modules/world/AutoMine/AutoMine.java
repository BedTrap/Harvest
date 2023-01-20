package me.eureka.harvest.systems.modules.world.AutoMine;

import me.eureka.harvest.events.event.BreakBlockEvent;
import me.eureka.harvest.events.event.Render3DEvent;
import me.eureka.harvest.events.event.StartBreakingBlockEvent;
import me.eureka.harvest.events.event.TickEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.ic.util.Renderer3D;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.modules.client.HUD.HUD;
import me.eureka.harvest.systems.utils.advanced.BlockPosX;
import me.eureka.harvest.systems.utils.advanced.FindItemResult;
import me.eureka.harvest.systems.utils.advanced.InvUtils;
import me.eureka.harvest.systems.utils.advanced.TargetUtils;
import me.eureka.harvest.systems.utils.other.TimerUtils;
import com.google.common.eventbus.Subscribe;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.List;

@Module.Info(name = "AutoMine", category = Module.Category.World)
public class AutoMine extends Module {
    public Setting<Boolean> autoCity = register("AutoCity", false);
    public Setting<Integer> offset = register("Offset", 0, 0, 200);
    public Setting<String> pickaxe = register("Pickaxe", List.of("NoDrop", "Fastest"), "Fastest");
    public Setting<Integer> actionDelay = register("ActionDelay", 0, 0, 5);
    public Setting<String> bypass = register("Bypass", List.of("Auto", "Manual", "OFF"), "Auto");
    public Setting<Boolean> crystal = register("Crystal", false);
    public Setting<Integer> age = register("Age", 1, 0, 3);
    public Setting<Boolean> limit = register("Limit", false);
    public Setting<Boolean> surroundOnly = register("SurroundOnly", false);
    public Setting<Boolean> fastBreak = register("FastBreak", false);
    public Setting<Boolean> haste = register("Haste", false);
    public Setting<Integer> amplifier = register("Amplifier", 2, 1, 2);
    public Setting<Boolean> ignoreAir = register("IgnoreAir", true);
    public Setting<Boolean> swing = register("Swing", true);
    public Setting<Boolean> debug = register("Debug", false);
    public Setting<Integer> alpha = register("Alpha", 140, 0, 255);

    private PlayerEntity target;

    private BlockPosX blockPos = null;
    private Direction direction = null;

    private FindItemResult crystalItem;
    private int pickSlot;

    private int breakTimes;

    private long start, total;
    private final TimerUtils timer = new TimerUtils();
    private final TimerUtils mineTimer = new TimerUtils();

    @Override
    public void onActivate() {
        breakTimes = 0;

        if (autoCity.get()) {
            target = TargetUtils.getPlayerTarget(6.5, TargetUtils.SortPriority.LowestDistance);
            if (TargetUtils.isBadTarget(target, 6)) {
                info("Target not found, ignoring...");
            } else {
                List<BlockPosX> blocks = PMUtils.getBlocksAround(target);
                blocks = blocks.stream().filter(blockPos -> {
                    if (blockPos.of(Blocks.BEDROCK)) return false;
                    return !blockPos.air();
                }).sorted(Comparator.comparingDouble(PMUtils::distanceTo)).toList();

                if (blocks.isEmpty()) {
                    info("Vulnerable positions is empty, ignoring...");
                } else {
                    blockPos = blocks.get(0);
                    direction = mc.player.getY() > blockPos.getY() ? Direction.UP : Direction.DOWN;

                    int slot = pickSlot();
                    if (slot == 420) {
                        info("Pickaxe not found.");
                        return;
                    }

                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction));
                    if (swing.get()) mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));

                    PMUtils.progress = 0;
                }
            }
        }
    }

    @Override
    public void onDeactivate() {
        blockPos = null;
        direction = null;
        if (mc.player.hasStatusEffect(StatusEffects.HASTE)) mc.player.removeStatusEffect(StatusEffects.HASTE);
    }

    @Subscribe
    public void onStartBreaking(StartBreakingBlockEvent event) {
        BlockPosX blockPos = new BlockPosX(event.blockPos);

        if (!blockPos.breakable()) return;
        if (surroundOnly.get() && !PMUtils.isPlayerNear(blockPos)) return;

        this.blockPos = blockPos;
        this.direction = event.direction;

        breakTimes = 0;
        PMUtils.progress = 0;

        start = System.currentTimeMillis();
        total = -1;
    }

    @Subscribe
    public void onBreak(BreakBlockEvent event) {
        if (!bypass.get("Manual")) return;

        if (this.crystal.get() && crystalItem.found()) swap(crystalItem.slot(), blockPos, true, false);
        swap(pickSlot, blockPos, false, false);
    }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        if (blockPos == null) return;
        addhaste(haste.get());

        crystalItem = InvUtils.find(Items.END_CRYSTAL);
        pickSlot = pickSlot();
        if (!PMUtils.canBreak(pickSlot, blockPos)) {
            timer.reset();

            total = (System.currentTimeMillis() - start) + offset.get();
            if (debug.get()) info(this.name, "Total: " + total);
            mineTimer.reset();
        } else {
            if (this.crystal.get() && crystalItem.found()) swap(crystalItem.slot(), blockPos, true, true);
            swap(pickSlot, blockPos, false, true);
        }

        if (bypass.get("Auto") && !blockPos.air() && blockPos.distance() < 5) {
            long total = this.total;

            long age = total - (this.age.get() * 20);
            if (mineTimer.passedMillis(age)) {
                if (this.crystal.get() && crystalItem.found()) swap(crystalItem.slot(), blockPos, true, false);
            }
            if (mineTimer.passedMillis(total)) {
                if (debug.get()) info(this.name, "AutoMining with speed " + total);
                swap(pickSlot, blockPos, false, false);
                mineTimer.reset();
            }
        }
    }

    private boolean swap(int slot, BlockPosX bp, boolean crystal, boolean check) {
        if (check && (slot == 420 || PMUtils.progress < 1 || (ignoreAir.get() && blockPos.air()) || (limit.get() && breakTimes >= 1) || !timer.passedTicks(actionDelay.get())))
            return false;

        move(mc.player.getInventory().selectedSlot, slot);
        if (crystal) {
            BlockHitResult hitResult = new BlockHitResult(blockPos.closestVec3d(), Direction.UP, blockPos.get(), false);
            if (blockPos.of(Blocks.OBSIDIAN) && PMUtils.isPlayerNear(blockPos)) {
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
            }
        } else {
            mine(bp);
        }
        move(mc.player.getInventory().selectedSlot, slot);
        timer.reset();
        return true;
    }

    private int pickSlot() {
        FindItemResult pick = pickaxe.get("Fastest") ? InvUtils.findFastestTool(blockPos.state()) : InvUtils.find(Items.GOLDEN_PICKAXE, Items.IRON_PICKAXE);
        return pick.found() ? pick.slot() : 420;
    }

    private void move(int from, int to) {
        ScreenHandler handler = mc.player.currentScreenHandler;

        Int2ObjectArrayMap<ItemStack> stack = new Int2ObjectArrayMap<>();
        stack.put(to, handler.getSlot(to).getStack());

        mc.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(handler.syncId, handler.getRevision(), PlayerInventory.MAIN_SIZE + from, to, SlotActionType.SWAP, handler.getCursorStack().copy(), stack));
    }

    private void mine(BlockPosX blockPos) {
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction));
        if (swing.get()) mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));

        if (fastBreak.get()) blockPos.state(Blocks.AIR);
        breakTimes++;
    }

    private void addhaste(boolean haste) {
        if (mc.player.hasStatusEffect(StatusEffects.HASTE) || !haste) return;

        mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 255, amplifier.get() - 1, false, false, true));
    }


    @Subscribe
    public void onRender(Render3DEvent event) {
        if (blockPos == null) return;
        if (limit.get() && breakTimes >= 1) return;

        int slot = pickSlot();
        if (slot == 420) return;

        double min = PMUtils.progress / 2;
        Vec3d vec3d = blockPos.center();
        Vec3d fixedVec = Renderer3D.getRenderPosition(vec3d);
        Box box = new Box(fixedVec.x - min, fixedVec.y - min, fixedVec.z - min, fixedVec.x + min, fixedVec.y + min, fixedVec.z + min);

        Renderer3D.get.drawBox(event.matrix(), box, HUD.INSTANCE.colorType(alpha.get()).getRGB());
        Renderer3D.get.drawOutlineBox(event.matrix(), box, HUD.INSTANCE.colorType(alpha.get()).getRGB());
    }
}
