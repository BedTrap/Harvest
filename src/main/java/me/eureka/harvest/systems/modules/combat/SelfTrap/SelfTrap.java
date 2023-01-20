package me.eureka.harvest.systems.modules.combat.SelfTrap;

import me.eureka.harvest.events.event.TickEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.utils.advanced.FindItemResult;
import me.eureka.harvest.systems.utils.advanced.InvUtils;
import com.google.common.eventbus.Subscribe;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

@Module.Info(name = "SelfTrap", category = Module.Category.Combat)
public class SelfTrap extends Module {

    public Setting<String> mode = register("Mode", List.of("Full", "Head", "Top"), "Top");
    public Setting<Integer> blockPerInterval = register("BlocksPerInterval", 2, 1, 5);
    public Setting<Integer> intervalDelay = register("IntervalDelay", 1, 0, 3);
    public Setting<Boolean> rotate = register("Rotate", false);
    public Setting<Boolean> autoDisable = register("AutoDisable", true);

    private int interval;

    @Override
    public void onActivate() {
        interval = 0;
    }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        if (autoDisable.get() && ((mc.options.jumpKey.isPressed() || mc.player.input.jumping) || mc.player.prevY < mc.player.getPos().getY())) {
            toggle();
            return;
        }

        //if (!FeetTrap.get.getDisplayInfo().equals("0")) return;

        if (interval > 0) interval--;
        if (interval > 0) return;

        ArrayList<BlockPos> poses = STUtils.getTrapBlocks(mode);
        setDisplayInfo(String.valueOf(poses.size()));
        FindItemResult block = InvUtils.findInHotbar(Items.OBSIDIAN);
        if (poses.isEmpty() || !block.found() || !STUtils.isSurrounded()) return;



        for (int i = 0; i <= blockPerInterval.get(); i++) {
            if (poses.size() > i) {
                STUtils.place(poses.get(i), rotate.get(),block.slot(), true);
            }
        }
        interval = intervalDelay.get();
    }
}
