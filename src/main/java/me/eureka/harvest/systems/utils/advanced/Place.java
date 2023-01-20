package me.eureka.harvest.systems.utils.advanced;

import me.eureka.harvest.ic.util.RotationUtil;
import me.eureka.harvest.mixininterface.IClientPlayerInteractionManager;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.modules.client.Rotations.Rotations;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import me.eureka.harvest.ic.util.Wrapper;
import me.eureka.harvest.systems.utils.game.ChatUtils;
import net.minecraft.block.*;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import static me.eureka.harvest.ic.util.Wrapper.mc;

public class Place {
    // Alternative usage
    private FindItemResult itemResult;
    private int slot;
    private Swap swap;
    private boolean packet, rotate, oldPlace, ignoreEntity, debug;

    public Place() {
        this.itemResult = null;
        this.slot = -1;
        this.swap = null;
        this.packet = false;
        this.rotate = false;
        this.oldPlace = false;
        this.ignoreEntity = false;
    }

    public Place item(FindItemResult itemResult) {
        this.itemResult = itemResult;
        return this;
    }

    public Place slot(int slot) {
        this.slot = slot;
        return this;
    }

    public Place swap(String swap) {
        this.swap(Place.by(swap));
        return this;
    }

    public Place swap(Swap swap) {
        this.swap = swap;
        return this;
    }

    public Place packet(boolean packet) {
        this.packet = packet;
        return this;
    }

    public Place rotate(boolean rotate) {
        this.rotate = rotate;
        return this;
    }

    public Place oldPlace(boolean oldPlace) {
        this.oldPlace = oldPlace;
        return this;
    }

    public Place ignoreEntity(boolean ignoreEntity) {
        this.ignoreEntity = ignoreEntity;
        return this;
    }

    public Place debug(boolean debug) {
        this.debug = debug;
        return this;
    }

    public void place(BlockPosX bp) {
        if (itemResult == null && slot != -1) {
            Place.place(bp, slot, swap, packet, rotate, oldPlace, ignoreEntity);
            return;
        } else if (itemResult != null && slot == -1) {
            Place.place(bp, itemResult, swap, packet, rotate, oldPlace, ignoreEntity);
            return;
        }

        if (debug) ChatUtils.info("Place", "Item missing (null)");
    }

    // General
    public static void place(BlockPosX bp, FindItemResult itemResult, Swap swap, boolean packet, boolean rotate, boolean oldPlace) {
        place(bp, itemResult.slot(), swap, packet, rotate, oldPlace, false);
    }

    public static void place(BlockPosX bp, int slot, Swap swap, boolean packet, boolean rotate, boolean oldPlace) {
        place(bp, slot, swap, packet, rotate, oldPlace, false);
    }

    public static void place(BlockPosX bp, FindItemResult itemResult, Swap swap, boolean packet, boolean rotate, boolean oldPlace, boolean ignoreEntity) {
        place(bp, itemResult.slot(), swap, packet, rotate, oldPlace, ignoreEntity);
    }

    public static void place(BlockPosX bp, int slot, Swap swap, boolean packet, boolean rotate, boolean oldPlace, boolean ignoreEntity) {
        if (oldPlace) {
            place$Old(bp, slot, swap, packet, rotate, ignoreEntity);
        } else place$New(bp, slot, swap, packet, rotate, ignoreEntity);
    }
    
    // 1.13+ place
    public static void place$New(BlockPosX bp, FindItemResult itemResult, Swap swap, boolean packet, boolean rotate, boolean ignoreEntity) {
        place$New(bp, itemResult.slot(), swap, packet, rotate, ignoreEntity);
    }

    public static void place$New(BlockPosX bp, int slot, Swap swap, boolean packet, boolean rotate, boolean ignoreEntity) {
        if (bp == null || slot == -1) return;
        if (!canPlace$New(bp, ignoreEntity)) return;

        int prevSlot = mc.player.getInventory().selectedSlot;
        Direction direction = placeDirection(bp);
        Hand hand = slot == 44 ? Hand.OFF_HAND : Hand.MAIN_HAND;

        boolean found = direction != null;
        if (found) {
            BlockPosX offset = bp.offset(direction);
            if (canPlace$Old(offset, ignoreEntity)) {
                if (rotate) Rotations.instance.rotate(bp);
                place$Normal(bp, slot, swap, packet, hand);
                return;
            }

            BlockHitResult hitResult = new BlockHitResult(offset.closestVec3d(), direction.getOpposite(), offset.get(), true);
            if (rotate) Rotations.instance.rotate(offset);

            switch (swap) {
                case Normal, Silent -> {
                    swap(slot);
                    place(hand, hitResult, packet, true);
                }
                case Move -> move(slot, () -> place(hand, hitResult, packet, true));
            }
        } else {
            if (rotate) Rotations.instance.rotate(bp);
            place$Normal(bp, slot, swap, packet, hand);
        }

        if (swap == Swap.Silent) swap(prevSlot);
    }

    private static void place$Normal(BlockPosX bp, int slot, Swap swap, boolean packet, Hand hand) {
        Direction direction = mc.player.getY() > bp.y() ? Direction.UP : Direction.DOWN;
        BlockHitResult hitResult = new BlockHitResult(bp.center(), direction, bp.get(), false);

        switch (swap) {
            case Normal, Silent -> {
                swap(slot);
                place(hand, hitResult, packet, false);
            }
            case Move -> move(slot, () -> place(hand, hitResult, packet, false));
        }
    }

    // 1.12 place
    public static void place$Old(BlockPosX bp, FindItemResult itemResult, Swap swap, boolean packet, boolean rotate, boolean ignoreEntity) {
        place$Old(bp, itemResult.slot(), swap, packet, rotate, ignoreEntity);
    }

    public static void place$Old(BlockPosX bp, int slot, Swap swap, boolean packet, boolean rotate, boolean ignoreEntity) {
        if (bp == null || slot == -1) return;
        if (!canPlace$Old(bp, ignoreEntity)) return;

        int prevSlot = mc.player.getInventory().selectedSlot;
        Direction direction = placeDirection(bp);
        if (direction == null) return;

        BlockPosX offset = bp.offset(direction);
        if (canPlace$Old(offset, ignoreEntity)) return;
        boolean shouldSneak = clickable(offset);

        Hand hand = slot == 44 ? Hand.OFF_HAND : Hand.MAIN_HAND;
        BlockHitResult hitResult = new BlockHitResult(offset.closestVec3d(), direction.getOpposite(), offset.get(), true);

        if (rotate) Rotations.instance.rotate(offset);
        switch (swap) {
            case Normal, Silent -> {
                swap(slot);
                place(hand, hitResult, packet, shouldSneak);
            }
            case Move -> move(slot, () -> place(hand, hitResult, packet, shouldSneak));
        }

        if (swap == Swap.Silent) swap(prevSlot);
    }

    // Misc
    // Returns direction of the closest block to bp;
    // Example: returns UP = bp.offset(UP), direction (opposite) DOWN;
    public static Direction placeDirection(BlockPosX bp) {
        double distance = 999.0;
        Direction direction = null;

        Direction clickable = null;
        for (Direction dir : Direction.values()) {
            BlockPosX cbp = bp.offset(dir);

            if (cbp.air() || cbp.replaceable() || cbp.fluid()) continue;
            if (clickable(cbp)) {
                clickable = dir;
                continue;
            }

            if (cbp.distance() < distance) {
                distance = cbp.distance();
                direction = dir;
            }
        }

        return direction == null ? clickable : direction;
    }

    private static void place(Hand hand, BlockHitResult hitResult, boolean packet, boolean sneak) {
        boolean shouldSneak = sneak && !mc.player.isSneaking();
        if (shouldSneak) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
            if (mc.player.getPose() == EntityPose.CROUCHING) mc.player.setPose(EntityPose.STANDING);
        }

        if (packet) {
            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(hand, hitResult, 0));
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
        } else {
            mc.interactionManager.interactBlock(mc.player, hand, hitResult);
            mc.player.swingHand(hand);
        }

        if (shouldSneak) mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
    }

//    public static boolean canPlace(BlockPosX bp, Direction direction, boolean oldPlace) {
//        if (oldPlace && bp.offset(direction).down().air()) return false;
//
//        return canPlace(bp);
//    }

    public static boolean canPlace(BlockPosX bp, boolean oldPlace, boolean ignoreEntity) {
        return oldPlace ? canPlace$Old(bp, ignoreEntity) : canPlace$New(bp, ignoreEntity);
    }

    private static boolean canPlace$New(BlockPosX bp, boolean ignoreEntity) {
        if (!World.isValid(bp.get())) return false;

        if (!ignoreEntity) {
            if (bp.hasEntity()) return false;
        }

        return (bp.replaceable() || bp.fluid());
    }

    private static boolean canPlace$Old(BlockPosX bp, boolean ignoreEntity) {
        if (!World.isValid(bp.get())) return false;

        if (!ignoreEntity) {
            if (bp.hasEntity()) return false;
        }

        return bp.replaceable();
    }

    public static boolean clickable(BlockPosX bp) {
        return bp.of(CraftingTableBlock.class)
                || bp.of(FurnaceBlock.class)
                || bp.of(AnvilBlock.class)
                || bp.of(AbstractButtonBlock.class)
                || bp.of(BlockWithEntity.class)
                || bp.of(BedBlock.class)
                || bp.of(FenceGateBlock.class)
                || bp.of(DoorBlock.class)
                || bp.of(NoteBlock.class)
                || bp.of(TrapdoorBlock.class)
                || bp.of(ChestBlock.class)
                || bp.of(Blocks.LODESTONE, Blocks.ENCHANTING_TABLE, Blocks.RESPAWN_ANCHOR, Blocks.LEVER, Blocks.BLAST_FURNACE, Blocks.BREWING_STAND, Blocks.LOOM, Blocks.CARTOGRAPHY_TABLE, Blocks.SMITHING_TABLE, Blocks.SMOKER, Blocks.STONECUTTER, Blocks.GRINDSTONE);
    }

    // Swap
    private static void swap(FindItemResult itemResult) {
        swap(itemResult.slot());
    }

    private static void swap(int slot) {
        int currentSlot = mc.player.getInventory().selectedSlot;
        if (currentSlot == slot) return;
        if (slot < 0 || slot > 8) return;

        mc.player.getInventory().selectedSlot = slot;
        ((IClientPlayerInteractionManager) mc.interactionManager).syncSelected();
    }

    private static void move(FindItemResult itemResult, Runnable runnable) {
        move(itemResult.slot(), runnable);
    }

    private static void move(int slot, Runnable runnable) {
        if (slot == -1 || slot == 44) {
            runnable.run();
            return;
        }

        move(mc.player.getInventory().selectedSlot, slot);
        runnable.run();
        move(mc.player.getInventory().selectedSlot, slot);
    }

    private static void move(int from, int to) {
        ScreenHandler handler = mc.player.currentScreenHandler;

        Int2ObjectArrayMap<ItemStack> stack = new Int2ObjectArrayMap<>();
        stack.put(to, handler.getSlot(to).getStack());

        mc.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(handler.syncId, handler.getRevision(), PlayerInventory.MAIN_SIZE + from, to, SlotActionType.SWAP, handler.getCursorStack().copy(), stack));
    }

    // Enum
    public static Swap by(String mode) {
        for (Swap swap : Swap.values()) {
            if (swap.name().equals(mode)) return swap;
        }

        return Swap.NotFound;
    }

    public enum Swap {
        Normal, Silent, Move, NotFound
    }

    public static void vanillaPlace(FindItemResult item_result, BlockPosX block_pos) {
        int prevSlot = mc.player.getInventory().selectedSlot;
        InvUtils.swap(item_result);

        BlockHitResult result = new BlockHitResult(block_pos.center(), Direction.DOWN, block_pos.get(), true);
        Module.mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result, 0));
        Module.mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        InvUtils.swap(prevSlot);
    }
}
