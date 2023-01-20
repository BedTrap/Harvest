package me.eureka.harvest.systems.modules.combat.AntiCev;

import com.google.common.eventbus.Subscribe;
import me.eureka.harvest.events.event.TickEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.utils.advanced.BlockPosX;
import me.eureka.harvest.systems.utils.advanced.FindItemResult;
import me.eureka.harvest.systems.utils.advanced.InvUtils;
import me.eureka.harvest.systems.utils.advanced.Place;
import me.eureka.harvest.systems.utils.anticheat.AntiCheat;
import me.eureka.harvest.systems.utils.anticheat.Click;
import me.eureka.harvest.systems.utils.game.PlayerState;
import me.eureka.harvest.systems.utils.other.TimerUtils;
import net.minecraft.block.Block;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;

@Module.Info(name = "AntiCev", category = Module.Category.Combat)
public class AntiCev extends Module {
    private final Setting<Integer> action_delay = register("ActionDelay", 0, 10, 500);
    private final Setting<Boolean> break_crystals = register("BreakCrystals", true);
    private final Setting<Boolean> place_on_break = register("PlaceOnBreak", false);
    private final Setting<Boolean> only_in_surround = register("OnlyInSurround", false);

    private final Setting<Boolean> packet_place = register("PacketPlace", true);
    private final Setting<Boolean> old_place = register("OldPlace", false);
    private final Setting<String> place_mode = register("PlaceMode", List.of("Strict", "Vanilla"), "Vanilla");
    private final Setting<String> blocks = register("Blocks", List.of("Obsidian", "AntiCrystal", "BlastResist"), "Obsidian");

    private final List<BlockPosX> positions = new ArrayList<>();
    private final TimerUtils timer = new TimerUtils();

    @Override
    public void onActivate() {
        timer.reset();
    }

    @Subscribe
    private void onTick(TickEvent.Post event) {
        onChecksPassed(() -> {
            generatePositions();
            placeBlocks();
        });
    }

    private void onChecksPassed(Runnable runnable) {
        if (only_in_surround.get() && !PlayerState.isSurrounded(mc.player, false)) return;

        runnable.run();
    }

    private void generatePositions() {
        BlockPosX start = new BlockPosX(mc.player.getEyePos().add(0.0, 1.0, 0.0));
        fillPositions(start);

        positions.removeIf((block_pos) -> (block_pos.hasEntity((entity) -> {
            if (entity instanceof EndCrystalEntity end_crystal) { // is crystal?
                BlockPosX entity_pos = new BlockPosX(end_crystal.getBlockPos());

                if (break_crystals.get()) AntiCheat.processClick(Click.M1, end_crystal);
                if (place_on_break.get()) placeBlock(entity_pos);
            }
        })) || !block_pos.replaceable());
    }

    private void placeBlocks() {
        if (positions.isEmpty()) return;

        if (timer.passedMillis(action_delay.get())) {
            BlockPosX current_block = positions.get(0);

            placeBlock(current_block);
            timer.reset();
        }
    }

    private void placeBlock(BlockPosX block_pos) {
        FindItemResult block = getBlock();

        switch (place_mode.get()) {
            case "Strict" -> Place.place(block_pos, block, Place.Swap.Silent, packet_place.get(), false, old_place.get(), true);
            case "Vanilla" -> Place.vanillaPlace(block, block_pos);
        }
    }

    private void fillPositions(BlockPosX start) {
        positions.clear();
        positions.add(start);

        positions.add(start.up());
        positions.add(start.north());
        positions.add(start.south());
        positions.add(start.west());
        positions.add(start.east());
    }

    private FindItemResult getBlock() {
        return switch (blocks.get()) {
            case "Obsidian" -> InvUtils.findInHotbar(Items.OBSIDIAN);
            case "AntiCrystal" -> InvUtils.findInHotbar(Items.NETHERITE_BLOCK, Items.CRYING_OBSIDIAN);
            case "BlastResist" -> InvUtils.findInHotbar(itemStack -> Block.getBlockFromItem(itemStack.getItem()).getBlastResistance() >= 600);
            default -> null;
        };
    }

    {
        old_place.setDescription("Allows you to place blocks without issues on 1.12, PlaceMode: Strict is required.");
    }
}
