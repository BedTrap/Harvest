package me.eureka.harvest.systems.modules.combat.AutoWeb;

import com.google.common.eventbus.Subscribe;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import me.eureka.harvest.Hack;
import me.eureka.harvest.events.event.Render3DEvent;
import me.eureka.harvest.events.event.TickEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.ic.util.Draw3D;
import me.eureka.harvest.ic.util.Renderer3D;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.modules.client.HUD.HUD;
import me.eureka.harvest.systems.modules.render.LogoutSpot.PlayerCopyEntity;
import me.eureka.harvest.systems.utils.advanced.BlockPosX;
import me.eureka.harvest.systems.utils.advanced.FindItemResult;
import me.eureka.harvest.systems.utils.advanced.InvUtils;
import me.eureka.harvest.systems.utils.advanced.Place;
import me.eureka.harvest.systems.utils.other.TimerUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static me.eureka.harvest.systems.modules.combat.FeetTrap.FTUtils.hasEntity;

@Module.Info(name = "AutoWeb", category = Module.Category.Combat)
public class AutoWeb extends Module {
    public Setting<String> swap = register("Swap", List.of("Normal", "Silent", "Move"), "Silent");
    public Setting<Boolean> inventory = register("Inventory", false);
    public Setting<Double> placeRange = register("PlaceRange", 4.5, 2, 7, 2);
    public Setting<Integer> actionDelay = register("ActionDelay", 10, 0, 300);
    public Setting<Integer> BPA = register("BPA", 1, 1, 5);
    public Setting<Boolean> packet = register("Packet", true);
    public Setting<Boolean> oldPlace = register("1.12", false);
    public Setting<Boolean> rotate = register("Rotate", false);
    public Setting<Boolean> render = register("Render", false);
    public Setting<String> shapeMode = register("ShapeMode", List.of("Line", "Fill", "Both"), "Both");
    public Setting<Integer> red = register("Red", 140, 0, 255);
    public Setting<Integer> green = register("Green", 140, 0, 255);
    public Setting<Integer> blue = register("Blue", 140, 0, 255);
    public Setting<Integer> alpha = register("Alpha", 140, 0, 255);

    private List<BlockPosX> positions = new ArrayList<>();
    private final TimerUtils timer = new TimerUtils();

    @Override
    public void onActivate() {
        positions.clear();

        timer.reset();
    }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        FindItemResult web = inventory.get() ? InvUtils.find(Items.COBWEB) : InvUtils.findInHotbar(Items.COBWEB);
        if (!web.found()) return;

        positions = positions();
        if (positions.isEmpty()) return;

        if (!timer.passedMillis(actionDelay.get())) return;

        int i = 0;
        for (BlockPosX bp : positions) {
            if (web.isHotbar()) {
                Place.place(bp, web, Place.by(swap.get()), packet.get(), rotate.get(), oldPlace.get(), true);
            } else if (inventory.get()) {
                move(web, () -> Place.place(bp, web, Place.Swap.Silent, packet.get(), rotate.get(), oldPlace.get(), true));
            }

            i++;
            if (i >= BPA.get()) break;
        }

        timer.reset();
    }

    private List<BlockPosX> positions() {
        List<BlockPosX> positions = new ArrayList<>();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player.isCreative() || player.isSpectator()) continue;
            if (player instanceof PlayerCopyEntity) continue;
            if (Hack.friends().isFriend(player)) continue;
            if (player == mc.player) continue;

            BlockPosX bp = new BlockPosX(player.getBlockPos());
            if (bp.distance() > placeRange.get()) continue;
            if (oldPlace.get()) {
                Direction direction = Place.placeDirection(bp);
                if (direction == null) continue;
            }

            if (bp.fluid() || bp.replaceable()) {
                //if (!intersects(player.getBoundingBox(), Blocks.COBWEB))
                positions.add(bp);
            }
        }
        return positions;
    }

    public boolean intersects(Box box, Block... blocks) {
        for (int x = (int) Math.floor(box.minX); x < Math.ceil(box.maxX); x++) {
            for (int y = (int) Math.floor(box.minY); y < Math.ceil(box.maxY); y++) {
                for (int z = (int) Math.floor(box.minZ); z < Math.ceil(box.maxZ); z++) {
                    BlockState state = mc.world.getBlockState(new BlockPos(x, y, z));

                    for (Block block : blocks) {
                        if (state.isOf(block)) return true;
                    }
                }
            }
        }

        return false;
    }

    public void move(FindItemResult itemResult, Runnable runnable) {
        if (itemResult.isOffhand()) {
            runnable.run();
            return;
        }

        move(mc.player.getInventory().selectedSlot, itemResult.slot());
        runnable.run();
        move(mc.player.getInventory().selectedSlot, itemResult.slot());
    }

    private void move(int from, int to) {
        assert mc.player != null;
        ScreenHandler handler = mc.player.currentScreenHandler;

        Int2ObjectArrayMap<ItemStack> stack = new Int2ObjectArrayMap<>();
        stack.put(to, handler.getSlot(to).getStack());

        mc.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(handler.syncId, handler.getRevision(), PlayerInventory.MAIN_SIZE + from, to, SlotActionType.SWAP, handler.getCursorStack().copy(), stack));
    }

    @Subscribe
    public void onRender(Render3DEvent event) {
        if (!render.get()) return;
        if (positions.isEmpty()) return;
        Color color = new Color(red.get(), green.get(), blue.get(), alpha.get());

        positions.forEach(bp -> {
            switch (shapeMode.get()) {
                case "Line" -> Draw3D.drawOutlineBox(event, bp.renderBox(), color);
                case "Fill" -> Draw3D.drawFilledBox(event, bp.renderBox(), color);
                case "Both" -> Draw3D.drawBox(event, bp.renderBox(), color);
            }
        });
    }
}
