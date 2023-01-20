package me.eureka.harvest.systems.modules.combat.HoleFill;

import com.google.common.eventbus.Subscribe;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import me.eureka.harvest.Hack;
import me.eureka.harvest.events.event.Render3DEvent;
import me.eureka.harvest.events.event.TickEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.ic.util.Draw3D;
import me.eureka.harvest.ic.util.Renderer3D;
import me.eureka.harvest.ic.util.Wrapper;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.modules.client.HUD.HUD;
import me.eureka.harvest.systems.modules.render.LogoutSpot.PlayerCopyEntity;
import me.eureka.harvest.systems.utils.advanced.BlockPosX;
import me.eureka.harvest.systems.utils.advanced.FindItemResult;
import me.eureka.harvest.systems.utils.advanced.InvUtils;
import me.eureka.harvest.systems.utils.advanced.Place;
import me.eureka.harvest.systems.utils.other.TimerUtils;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Module.Info(name = "HoleFill", category = Module.Category.Combat)
public class HoleFill extends Module {
    public Setting<String> swap = register("Swap", List.of("Normal", "Silent", "Move"), "Silent");
    public Setting<Boolean> inventory = register("Inventory", false);
    public Setting<Integer> BPA = register("BPA", 1, 1, 5);
    public Setting<Integer> actionDelay = register("ActionDelay", 10, 0, 300);
    public Setting<Boolean> packet = register("Packet", true);
    public Setting<Boolean> rotate = register("Rotate", false);
    public Setting<Double> placeRange = register("PlaceRange", 4.5, 2, 7, 2);
    public Setting<Double> targetRange = register("TargetRange", 3, 1, 5, 2);
    public Setting<String> priority = register("Priority", List.of("Web", "Obsidian", "Netherite", "Any"), "Any");
    public Setting<Boolean> doubleHole = register("DoubleHole", true);
    public Setting<Boolean> self = register("Self", false);
    public Setting<Boolean> render = register("Render", false);
    public Setting<String> shapeMode = register("ShapeMode", List.of("Line", "Fill", "Both"), "Both");
    public Setting<Integer> red = register("Red", 140, 0, 255);
    public Setting<Integer> green = register("Green", 140, 0, 255);
    public Setting<Integer> blue = register("Blue", 140, 0, 255);
    public Setting<Integer> alpha = register("Alpha", 140, 0, 255);

    private List<PlayerEntity> targets = new ArrayList<>();
    private List<BlockPosX> holes = new ArrayList<>();

    private final TimerUtils timer = new TimerUtils();

    @Override
    public void onActivate() {
        targets.clear();

        timer.reset();
    }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        targets = targets();
        if (targets.isEmpty()) return;

        FindItemResult block = getBlock();
        if (!block.found()) return;

        holes = this.getSphere(mc.player, placeRange.get());
        holes = holes.stream().filter(bp -> {
            if (bp.distance() > placeRange.get()) return false;
            if (bp.hasEntity()) return false;
            if (!intersects(bp)) return false;

            if (self.get()) {

                if (bp.equals(BlockPosX.feetPos().up(mc.player.getPose() == EntityPose.SWIMMING ? 1 : 2))) return true;
            }

            return (Hole.singleHole(bp) || (doubleHole.get() && Hole.doubleHole(bp, true)));
        }).toList();

        if (holes.isEmpty()) return;

        if (!timer.passedMillis(actionDelay.get())) return;

        int i = 0;
        for (BlockPosX bp : holes) {
            if (bp.hasEntity()) continue;

            if (block.isHotbar()) {
                Place.place(bp, block, Place.by(swap.get()), packet.get(), rotate.get(), true);
            } else if (inventory.get()) {
                move(block, () -> Place.place(bp, block, Place.Swap.Silent, packet.get(), rotate.get(), true));
            }

            i++;
            if (i >= BPA.get()) break;
        }

        timer.reset();
    }

    private FindItemResult getBlock() {
        FindItemResult itemResult = null;

        switch (priority.get()) {
            case "Web" ->
                    itemResult = inventory.get() ? InvUtils.find(Items.COBWEB) : InvUtils.findInHotbar(Items.COBWEB);
            case "Obsidian" ->
                    itemResult = inventory.get() ? InvUtils.find(Items.OBSIDIAN) : InvUtils.findInHotbar(Items.OBSIDIAN);
            case "Netherite" ->
                    itemResult = inventory.get() ? InvUtils.find(Items.NETHERITE_BLOCK) : InvUtils.findInHotbar(Items.NETHERITE_BLOCK);
        }
        if (!priority.get("Any") && itemResult.found()) return itemResult;

        return inventory.get()
                ? InvUtils.find(itemStack -> Block.getBlockFromItem(itemStack.getItem()).getBlastResistance() >= 600)
                : InvUtils.findInHotbar(itemStack -> Block.getBlockFromItem(itemStack.getItem()).getBlastResistance() >= 600);
    }

    public boolean intersects(BlockPosX bp) {
        if (targets.isEmpty()) return false;

        for (PlayerEntity target : targets) {
            Vec3d feetPos = new Vec3d(target.getX(), target.getY(), target.getZ());

            if (feetPos.isInRange(bp.vec3d(), targetRange.get())) return true;
        }

        return false;
    }

    public List<BlockPosX> getSphere(PlayerEntity player, double radius) {
        Vec3d eyePos = new Vec3d(player.getX(), player.getY() + player.getEyeHeight(player.getPose()), player.getZ());
        List<BlockPosX> blocks = new ArrayList<>();

        for (double i = eyePos.getX() - radius; i < eyePos.getX() + radius; i++) {
            for (double j = eyePos.getY() - radius; j < eyePos.getY() + radius; j++) {
                for (double k = eyePos.getZ() - radius; k < eyePos.getZ() + radius; k++) {
                    Vec3d vecPos = new Vec3d(i, j, k);

                    // Closest Vec3d
                    double x = MathHelper.clamp(eyePos.getX() - vecPos.getX(), 0.0, 1.0);
                    double y = MathHelper.clamp(eyePos.getY() - vecPos.getY(), 0.0, 1.0);
                    double z = MathHelper.clamp(eyePos.getZ() - vecPos.getZ(), 0.0, 1.0);
                    Vec3d vec3d = new Vec3d(eyePos.getX() + x, eyePos.getY() + y, eyePos.getZ() + z);

                    // Distance to Vec3d
                    float f = (float) (eyePos.getX() - vec3d.x);
                    float g = (float) (eyePos.getY() - vec3d.y);
                    float h = (float) (eyePos.getZ() - vec3d.z);

                    double distance = MathHelper.sqrt(f * f + g * g + h * h);

                    if (distance > radius) continue;
                    BlockPosX blockPos = new BlockPosX(vecPos);

                    if (blocks.contains(blockPos)) continue;
                    blocks.add(blockPos);
                }
            }
        }

        return blocks.stream().filter(BlockPosX::air).toList();
    }

    private List<PlayerEntity> targets() {
        List<PlayerEntity> targets = new ArrayList<>();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player instanceof PlayerCopyEntity) continue;
            if (player.isCreative() || player.isSpectator()) continue;
            if (player.isDead() || !player.isAlive()) continue;
            if (Hack.friends().isFriend(player)) continue;
            if (player == mc.player) continue;

            targets.add(player);
        }

        return targets;
    }

    public void move(FindItemResult itemResult, Runnable runnable) {
        if (itemResult.isOffhand()) {
            runnable.run();
            return;
        }

        move(Wrapper.mc.player.getInventory().selectedSlot, itemResult.slot());
        runnable.run();
        move(Wrapper.mc.player.getInventory().selectedSlot, itemResult.slot());
    }

    private void move(int from, int to) {
        ScreenHandler handler = mc.player.currentScreenHandler;

        Int2ObjectArrayMap<ItemStack> stack = new Int2ObjectArrayMap<>();
        stack.put(to, handler.getSlot(to).getStack());

        Wrapper.mc.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(handler.syncId, handler.getRevision(), PlayerInventory.MAIN_SIZE + from, to, SlotActionType.SWAP, handler.getCursorStack().copy(), stack));
    }

    public boolean isSurrounded(PlayerEntity player) {
        ArrayList<BlockPos> positions = new ArrayList<>();
        List<Entity> getEntityBoxes;

        for (BlockPos blockPos : getSphere(player, 4)) {
            if (!mc.world.getBlockState(blockPos).getMaterial().isReplaceable()) continue;
            getEntityBoxes = mc.world.getOtherEntities(null, new Box(blockPos), entity -> entity == player);
            if (!getEntityBoxes.isEmpty()) continue;

            for (Direction direction : Direction.values()) {
                if (direction == Direction.UP || direction == Direction.DOWN) continue;

                getEntityBoxes = mc.world.getOtherEntities(null, new Box(blockPos.offset(direction)), entity -> entity == player);
                if (!getEntityBoxes.isEmpty()) positions.add(blockPos);
            }
        }

        positions.removeIf(bp -> new BlockPosX(bp).air());
        return !positions.isEmpty();
    }

    @Subscribe
    public void onRender(Render3DEvent event) {
        if (!render.get()) return;
        if (holes.isEmpty()) return;

        holes.forEach(bp -> {
            Color color = new Color(red.get(), green.get(), blue.get(), alpha.get());

            switch (shapeMode.get()) {
                case "Line" -> Draw3D.drawOutlineBox(event, bp.renderBox(), color);
                case "Fill" -> Draw3D.drawFilledBox(event, bp.renderBox(), color);
                case "Both" -> Draw3D.drawBox(event, bp.renderBox(), color);
            }
        });
    }
}