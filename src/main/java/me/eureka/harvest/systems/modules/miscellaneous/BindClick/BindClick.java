package me.eureka.harvest.systems.modules.miscellaneous.BindClick;

import me.eureka.harvest.events.event.TickEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.systems.modules.Module;
import com.google.common.eventbus.Subscribe;
import me.eureka.harvest.systems.utils.advanced.FindItemResult;
import me.eureka.harvest.systems.utils.advanced.InvUtils;

import net.minecraft.item.Items;
import net.minecraft.util.Hand;


import java.util.List;

@Module.Info(name = "BindClick", category = Module.Category.Miscellaneous)
public class BindClick extends Module {
    public Setting<String> mode = register("Mode", List.of("Pearl", "Rocket"), "Pearl");

    private FindItemResult item;

    @Subscribe
    private void onTick(TickEvent.Pre event) {
        switch (mode.get()) {
            case "Pearl" -> item = InvUtils.findInHotbar(Items.ENDER_PEARL);
            case "Rocket" -> item = InvUtils.findInHotbar(Items.FIREWORK_ROCKET);
        }

        if (item.found()) {
            InvUtils.swap(item.slot(), true);
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            InvUtils.swapBack();
        }

        toggle();
    }
}
