package me.eureka.harvest.systems.commands;

import me.eureka.harvest.Hack;
import me.eureka.harvest.ic.Command;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.utils.game.ChatUtils;


public class DrawnCommand extends Command {

    boolean found = false;

    public DrawnCommand() {
        super(new String[]{"d", "drawn"});
    }

    @Override
    public void onCommand(String name, String[] args) {
        if (args.length == 0) {
            ChatUtils.info("Drawn","Invalid input. Usage: " + getPrefix() + "drawn <Module>");
            return;
        }

        Module module = Hack.modules().get(args[0]);
        if (module != null) {
            module.setDrawn(!module.drawn);
            if (module.isDrawn())
                ChatUtils.info("Drawn",module.getName() + " now drawn.");
            else
                ChatUtils.info("Drawn",module.getName() + " is no more drawn.");
        } else
            ChatUtils.info("Drawn","Module isn't existing.");
    }

}
