package me.eureka.harvest.systems.utils.advanced;

import me.eureka.harvest.ic.util.Wrapper;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.AirBlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

import java.util.function.Predicate;

public class InvUtils {
    public static final PlayerInventory inventory = Wrapper.mc.player.getInventory();
    private static final Action ACTION = new Action();
    public static int previousSlot = -1;

    // Finding items

    public static boolean isInventoryFull() {
        for (int i = 0; i < 36; i++) {
            ItemStack itemStack = Wrapper.mc.player.getInventory().getStack(i);
            if (itemStack == null || itemStack.getItem() instanceof AirBlockItem) return false;
        }
        return true;
    }

    public static Integer getEmptySlots() {
        int emptySlots = 0;
        for (int i = 0; i < 36; i++) if (isSlotEmpty(i)) emptySlots++;
        return emptySlots;
    }

    public static boolean isSlotEmpty(Integer slot) {
        ItemStack itemStack = getStackFromSlot(slot);
        if (itemStack == null) return true;
        return itemStack.getItem() instanceof AirBlockItem;
    }

    public static ItemStack getStackFromSlot(Integer slot) {
        if (slot == -1) return null;
        return Wrapper.mc.player.getInventory().getStack(slot);
    }

    public static FindItemResult findEmpty() {
        return find(ItemStack::isEmpty);
    }

    public static FindItemResult findInHotbar(Item... items) {
        return findInHotbar(itemStack -> {
            for (Item item : items) {
                if (itemStack.getItem() == item) return true;
            }
            return false;
        });
    }

    public static FindItemResult findInHotbar(Predicate<ItemStack> isGood) {
        if (isGood.test(Wrapper.mc.player.getOffHandStack())) {
            return new FindItemResult(SlotUtils.OFFHAND, Wrapper.mc.player.getOffHandStack().getCount());
        }

        if (isGood.test(Wrapper.mc.player.getMainHandStack())) {
            return new FindItemResult(Wrapper.mc.player.getInventory().selectedSlot, Wrapper.mc.player.getMainHandStack().getCount());
        }

        return find(isGood, 0, 8);
    }

    public static FindItemResult find(Item... items) {
        return find(itemStack -> {
            for (Item item : items) {
                if (itemStack.getItem() == item) return true;
            }
            return false;
        });
    }

    public static FindItemResult find(Predicate<ItemStack> isGood) {
        return find(isGood, 0, Wrapper.mc.player.getInventory().size());
    }

    public static FindItemResult find(Predicate<ItemStack> isGood, int start, int end) {
        int slot = -1, count = 0;

        for (int i = start; i <= end; i++) {
            ItemStack stack = Wrapper.mc.player.getInventory().getStack(i);

            if (isGood.test(stack)) {
                if (slot == -1) slot = i;
                count += stack.getCount();
            }
        }

        return new FindItemResult(slot, count);
    }

    public static FindItemResult findFastestTool(BlockState state) {
        float bestScore = -1;
        int slot = -1;

        for (int i = 0; i < Wrapper.mc.player.getInventory().size(); i++) {
            float score = Wrapper.mc.player.getInventory().getStack(i).getMiningSpeedMultiplier(state);
            if (score > bestScore) {
                bestScore = score;
                slot = i;
            }
        }

        return new FindItemResult(slot, 1);
    }

    public static FindItemResult findFastestToolInHotbar(BlockState state) {
        float bestScore = -1;
        int slot = -1;

        for (int i = 0; i < 9; i++) {
            float score = Wrapper.mc.player.getInventory().getStack(i).getMiningSpeedMultiplier(state);
            if (score > bestScore) {
                bestScore = score;
                slot = i;
            }
        }

        return new FindItemResult(slot, 1);
    }

    // Interactions
    public static boolean swap(FindItemResult itemResult) {
        return swap(itemResult.slot());
    }

    public static boolean swap(int slot) {
        if (slot < 0 || slot > 8) return false;
        previousSlot = Wrapper.mc.player.getInventory().selectedSlot;

        Wrapper.mc.player.getInventory().selectedSlot = slot;
        Wrapper.mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        return true;
    }

    public static boolean swap(int slot, boolean swapBack) {
        if (slot < 0 || slot > 8) return false;
        if (swapBack && previousSlot == -1) previousSlot = Wrapper.mc.player.getInventory().selectedSlot;

        Wrapper.mc.player.getInventory().selectedSlot = slot;
        return true;
    }

    public static void syncSlots() {
        Wrapper.mc.player.getInventory().selectedSlot = Wrapper.mc.player.getInventory().selectedSlot;
        Wrapper.mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(Wrapper.mc.player.getInventory().selectedSlot));
        Wrapper.mc.player.getInventory().updateItems();
    }

    public static boolean swapBack() {
        if (previousSlot == -1) return false;

        boolean return_ = swap(previousSlot, false);
        previousSlot = -1;
        return return_;
    }

    public static Action move() {
        ACTION.holeType = SlotActionType.PICKUP;
        ACTION.two = true;
        return ACTION;
    }

    public static Action click() {
        ACTION.holeType = SlotActionType.PICKUP;
        return ACTION;
    }

    public static Action quickMove() {
        ACTION.holeType = SlotActionType.QUICK_MOVE;
        return ACTION;
    }

    public static Action drop() {
        ACTION.holeType = SlotActionType.THROW;
        ACTION.data = 1;
        return ACTION;
    }

    public static void dropHand() {
        if (!Wrapper.mc.player.currentScreenHandler.getCursorStack().isEmpty()) Wrapper.mc.interactionManager.clickSlot(Wrapper.mc.player.currentScreenHandler.syncId, ScreenHandler.EMPTY_SPACE_SLOT_INDEX, 0, SlotActionType.PICKUP, Wrapper.mc.player);
    }

    public static class Action {
        private SlotActionType holeType = null;
        private boolean two = false;
        private int from = -1;
        private int to = -1;
        private int data = 0;

        private boolean isRecursive = false;

        private Action() {}

        // From

        public Action fromId(int id) {
            from = id;
            return this;
        }

        public Action from(int index) {
            return fromId(SlotUtils.indexToId(index));
        }

        public Action fromHotbar(int i) {
            return from(SlotUtils.HOTBAR_START + i);
        }

        public Action fromOffhand() {
            return from(SlotUtils.OFFHAND);
        }

        public Action fromMain(int i) {
            return from(SlotUtils.MAIN_START + i);
        }

        public Action fromArmor(int i) {
            return from(SlotUtils.ARMOR_START + (3 - i));
        }

        // To

        public void toId(int id) {
            to = id;
            run();
        }

        public void to(int index) {
            toId(SlotUtils.indexToId(index));
        }

        public void toHotbar(int i) {
            to(SlotUtils.HOTBAR_START + i);
        }

        public void toOffhand() {
            to(SlotUtils.OFFHAND);
        }

        public void toMain(int i) {
            to(SlotUtils.MAIN_START + i);
        }

        public void toArmor(int i) {
            to(SlotUtils.ARMOR_START + (3 - i));
        }

        // Slot

        public void slotId(int id) {
            from = to = id;
            run();
        }

        public void slot(int index) {
            slotId(SlotUtils.indexToId(index));
        }

        public void slotHotbar(int i) {
            slot(SlotUtils.HOTBAR_START + i);
        }

        public void slotOffhand() {
            slot(SlotUtils.OFFHAND);
        }

        public void slotMain(int i) {
            slot(SlotUtils.MAIN_START + i);
        }

        public void slotArmor(int i) {
            slot(SlotUtils.ARMOR_START + (3 - i));
        }

        // Other

        private void run() {
            boolean hadEmptyCursor = Wrapper.mc.player.currentScreenHandler.getCursorStack().isEmpty();

            if (holeType != null && from != -1 && to != -1) {
                click(from);
                if (two) click(to);
            }

            SlotActionType preType = holeType;
            boolean preTwo = two;
            int preFrom = from;
            int preTo = to;

            holeType = null;
            two = false;
            from = -1;
            to = -1;
            data = 0;

            if (!isRecursive && hadEmptyCursor && preType == SlotActionType.PICKUP && preTwo && (preFrom != -1 && preTo != -1) && !Wrapper.mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                isRecursive = true;
                InvUtils.click().slotId(preFrom);
                isRecursive = false;
            }
        }

        private void click(int id) {
            Wrapper.mc.interactionManager.clickSlot(Wrapper.mc.player.currentScreenHandler.syncId, id, data, holeType, Wrapper.mc.player);
        }
    }
}