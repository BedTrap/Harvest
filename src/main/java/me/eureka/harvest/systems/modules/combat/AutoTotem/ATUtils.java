package me.eureka.harvest.systems.modules.combat.AutoTotem;

import me.eureka.harvest.systems.managers.DamageManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;

import static me.eureka.harvest.ic.util.Wrapper.mc;

public class ATUtils {

    public static Item totemOverride;
    public static float getHealth() {
        return mc.player.getHealth() + mc.player.getAbsorptionAmount();
    }

    public static int Idx2Id(int idx, ScreenHandler handler) {
        if (handler instanceof PlayerScreenHandler) {
            if (PlayerInventory.isValidHotbarIndex(idx)) {
                return idx + 36;
            }
            if (IsMain(idx)) {
                return idx;
            }
        } else {
            if (PlayerInventory.isValidHotbarIndex(idx)) {
                return idx + handler.slots.size() - 9;
            }
            if (IsMain(idx)) {
                return idx + handler.slots.size() - 45;
            }
        }
        return -1;
    }

    private static boolean IsMain(int i) {
        return i >= 9 && i < 36;
    }
    public static void totemItem(Item item){
        totemOverride = item;
    }
    public static void totemItem(){
        totemOverride = null;
    }

    public static double possibleHealthReductions(boolean entities, boolean fall) {
        double damageTaken = 0;

        if (entities) {
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof EndCrystalEntity && damageTaken < DamageManager.crystalDamage(mc.player, entity.getPos())) {
                    damageTaken = DamageManager.crystalDamage(mc.player, entity.getPos());
                }
            }
        }

        if (fall) {
            if (mc.player.fallDistance > 3) {
                double damage = mc.player.fallDistance * 0.5;

                if (damage > damageTaken) {
                    damageTaken = damage;
                }
            }
        }

        return damageTaken;
    }
}
