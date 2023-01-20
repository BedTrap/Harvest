package me.eureka.harvest.systems.commands;

import me.eureka.harvest.ic.Command;
import me.eureka.harvest.systems.modules.client.MPvP.MPvP;
import me.eureka.harvest.systems.utils.game.ChatUtils;

public class MPvPCommand extends Command {

    public MPvPCommand() {
        super(new String[]{"mpvp"});
    }

    @Override
    public void onCommand(String name, String[] args) {
        if (args[0].isEmpty() || args[0].isBlank()) {
            ChatUtils.info("MPvP", "Name is null.");
            return;
        }

        if (!MPvP.INSTANCE.isActive()) {
            ChatUtils.info("MPvP", "AutoMPVP is toggled off.");
            return;
        }

        MPvP.INSTANCE.player = args[0];
        ChatUtils.info("MPvP", "Trollage started on " + args[0]);
    }
}
