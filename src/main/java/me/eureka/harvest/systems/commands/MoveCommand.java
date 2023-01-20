package me.eureka.harvest.systems.commands;

import me.eureka.harvest.ic.Command;
import me.eureka.harvest.systems.utils.game.ChatUtils;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.SlotActionType;

public class MoveCommand extends Command {

    public MoveCommand() {
        super(new String[]{"move"});
    }

    @Override
    public void onCommand(String name, String[] args) {
        if (args[0] == null) return;

        int slot = -0;

        try {
            slot = Integer.parseInt(args[0]);
        } catch (Exception e) {
            e.fillInStackTrace();
        }

        if (slot == -0) {
            ChatUtils.info("Move", "Wrong slot.");
            return;
        }
        move(slot);
    }

    public static void move(int slot) {
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, PlayerInventory.MAIN_SIZE + mc.player.getInventory().selectedSlot, slot, SlotActionType.SWAP, mc.player);
    }
}