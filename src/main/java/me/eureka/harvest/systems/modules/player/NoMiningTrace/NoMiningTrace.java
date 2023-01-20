package me.eureka.harvest.systems.modules.player.NoMiningTrace;

import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.systems.modules.Module;
import net.minecraft.item.PickaxeItem;

@Module.Info(name = "NoMiningTrace", category = Module.Category.Player)
public class NoMiningTrace extends Module {
    public Setting<Boolean> onlyPickaxe = register("OnlyPickaxe", true);

    public boolean canWork() {
        if (!isActive()) return false;

        if (onlyPickaxe.get()) return mc.player.getMainHandStack().getItem() instanceof PickaxeItem;
        return true;
    }

    public static NoMiningTrace instance;

    public NoMiningTrace() {
        instance = this;
    }
}
