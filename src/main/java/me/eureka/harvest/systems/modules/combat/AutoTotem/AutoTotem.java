package me.eureka.harvest.systems.modules.combat.AutoTotem;

import me.eureka.harvest.events.event.GameJoinedEvent;
import me.eureka.harvest.events.event.TickEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.utils.advanced.InvUtils;
import com.google.common.eventbus.Subscribe;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import static me.eureka.harvest.systems.modules.combat.AutoTotem.ATUtils.*;

@Module.Info(name = "AutoTotem", category = Module.Category.Combat)
public class AutoTotem extends Module {

    public Setting<Boolean> strict = register("Strict", false);
    public Setting<Integer> health = register("Health", 15, 1, 36);
    public Setting<Integer> ping = register("Ping", 200, 100, 1500);
    public Setting<Boolean> onLag = register("OnServerLag", false);
    public Setting<Boolean> onFall = register("onFalling", true);
    public Setting<Boolean> hotbarExclude = register("HotbarExclude", true);
    //public Setting<Boolean> containerSafety = register("ContainerSafety", false);

    // TODO: 07.05.2022 onlag sloman
    private long timeLastTimeUpdate = -1;
    private long timeGameJoined;

    @Subscribe
    public void onTick(TickEvent.Post event) {
        setDisplayInfo(String.valueOf(InvUtils.find(Items.TOTEM_OF_UNDYING).count()));
        if (mc.player.currentScreenHandler instanceof CreativeInventoryScreen.CreativeScreenHandler) return;

        if (mc.player.getOffHandStack().getItem() == item()) return;

//        if (item() == Items.TOTEM_OF_UNDYING && containerSafety.get() && mc.player.currentScreenHandler instanceof GenericContainerScreenHandler){
//            mc.options.inventoryKey.setPressed(true);
//            mc.options.inventoryKey.setPressed(false);
//            ChatUtils.info("s");
//
//        }

        if (mc.player.currentScreenHandler.getCursorStack().getItem() == item()) {
            if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler))
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 45, 0, SlotActionType.PICKUP, mc.player);
            return;
        }

        int idx = -1;
        int id = -1;
        for (int i = 0; i < 36; ++i) {
            ItemStack itemstack;
            int id2 = Idx2Id(i, mc.player.currentScreenHandler);
            if (id2 < 0 || id2 >= mc.player.playerScreenHandler.slots.size() || (itemstack = (mc.player.playerScreenHandler.slots.get(id2)).getStack()).isEmpty() || itemstack.getItem() != item())
                continue;
            if (hotbarExclude.get() && Idx2Id(i, mc.player.currentScreenHandler) >= 36 && Idx2Id(i, mc.player.currentScreenHandler) <= 44)
                continue;

            idx = i;
            break;
        }

        if (idx == -1 && !(mc.player.currentScreenHandler instanceof PlayerScreenHandler)) {
            for (Slot slot : mc.player.currentScreenHandler.slots) {
                if (slot.getStack().isEmpty() || slot.getStack().getItem() != item()) continue;
                id = slot.id;
                break;
            }
        }

        if (id == -1) {
            if (idx == -1) {
                return;
            }
            id = Idx2Id(idx, mc.player.currentScreenHandler);
        }
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, id, 40, SlotActionType.SWAP, mc.player);
    }

    @Subscribe
    private void onGameJoined(GameJoinedEvent event) {
        timeGameJoined = timeLastTimeUpdate = System.currentTimeMillis();
    }

    private float getTimeSinceLastTick() {
        long now = System.currentTimeMillis();
        if (now - timeGameJoined < 4000) return 0;
        return (now - timeLastTimeUpdate) / 1000f;
    }

    private Item item() {
        if (strict.get() || mc.player.isFallFlying() || getHealth() - possibleHealthReductions(true,onFall.get()) < health.get() || mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()).getLatency() > ping.get() || getTimeSinceLastTick() > 1 && onLag.get())
            return Items.TOTEM_OF_UNDYING;
        if (ATUtils.totemOverride != null) return ATUtils.totemOverride;
        return Items.END_CRYSTAL;
    }
}
