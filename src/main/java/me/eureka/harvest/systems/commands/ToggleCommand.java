package me.eureka.harvest.systems.commands;

import me.eureka.harvest.Hack;
import me.eureka.harvest.ic.Command;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.utils.game.ChatUtils;
import net.minecraft.util.Formatting;


public class ToggleCommand extends Command {

    public ToggleCommand() {
        super(new String[]{"toggle", "t"});
    }

    @Override
    public void onCommand(String name, String[] args) {
        if (args.length == 0) {
            ChatUtils.info("Toggle", "Invalid input! Usage: " + getPrefix() + "t <Module>");
            return;
        }

        Module module = Hack.modules().get(args[0]);
        if (module != null) {
            module.toggle();
            if (module.isActive()) ChatUtils.info("Toggle", module.getName() + " toggled " + Formatting.GREEN + " on" + Formatting.GRAY + ".");
            else ChatUtils.info("Toggle", module.getName() + " toggled " + Formatting.RED + " off" + Formatting.GRAY + ".");
        } else ChatUtils.info("Toggle", "Module isn't existing.");
    }
}
