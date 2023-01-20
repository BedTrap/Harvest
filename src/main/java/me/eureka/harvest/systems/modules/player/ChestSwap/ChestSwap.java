package me.eureka.harvest.systems.modules.player.ChestSwap;

import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.utils.advanced.InvUtils;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

@Module.Info(name = "ChestSwap", category = Module.Category.Player)
public class ChestSwap extends Module {
    //public Setting<Boolean> BRUH = register("BRUH", true);

    @Override
    public void onActivate() {
        //if (!BRUH.get()) {
            swap();
            toggle();
        //}
    }

//    @Subscribe
//    public void onTick(TickEvent.Post event) {
//        Item currentItem = mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem();
//
//        if (currentItem != Items.ELYTRA) swap();
//        else {
//            if (!mc.player.isFallFlying()) {
//                mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
//            } else {
//                mc.player.setPosition(mc.player.getX(), mc.player.getY() + 10, mc.player.getZ());
//            }
//        }
//    }

    public void swap() {
        Item currentItem = mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem();

        if (currentItem == Items.ELYTRA) {
            equipChestplate();
        } else if (currentItem instanceof ArmorItem && ((ArmorItem) currentItem).getSlotType() == EquipmentSlot.CHEST) {
            equipElytra();
        } else {
            if (!equipChestplate()) equipElytra();
        }
    }

    private boolean equipChestplate() {
        int bestSlot = -1;
        boolean breakLoop = false;

        for (int i = 0; i < mc.player.getInventory().main.size(); i++) {
            Item item = mc.player.getInventory().main.get(i).getItem();

            if (item == Items.DIAMOND_CHESTPLATE) {
                bestSlot = i;
            } else if (item == Items.NETHERITE_CHESTPLATE) {
                bestSlot = i;
                breakLoop = true;
            }

            if (breakLoop) break;
        }

        if (bestSlot != -1) equip(bestSlot);
        return bestSlot != -1;
    }

    private void equipElytra() {
        for (int i = 0; i < mc.player.getInventory().main.size(); i++) {
            Item item = mc.player.getInventory().main.get(i).getItem();

            if (item == Items.ELYTRA) {
                equip(i);
                break;
            }
        }
    }

    private void equip(int slot) {
        InvUtils.move().from(slot).toArmor(2);
    }
}
