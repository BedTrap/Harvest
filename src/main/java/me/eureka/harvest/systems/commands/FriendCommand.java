package me.eureka.harvest.systems.commands;

import me.eureka.harvest.Hack;
import me.eureka.harvest.ic.Command;
import me.eureka.harvest.systems.utils.game.ChatUtils;

public class FriendCommand extends Command {

    public FriendCommand() {
        super(new String[]{"f", "friend"});
    }

    @Override
    public void onCommand(String name, String[] args) {
        if (args.length < 2) {
            ChatUtils.info("Friends","Invalid input. Usage: " + getPrefix() + "f <add/del> <name>");
            return;
        }

        if (args[0].equalsIgnoreCase("add")) {
            if (Hack.friends().isFriend(args[1])) {
                ChatUtils.info("Friends",args[1] + " is already in your friend list.");
            } else {
                Hack.friends().friends().add(args[1]);
                ChatUtils.info("Friends",args[1] + " is added to the friend list.");
            }
        } else if (args[0].equalsIgnoreCase("del") || args[0].equalsIgnoreCase("remove")) {
            if (Hack.friends().friends().isEmpty()) {
                ChatUtils.info("Friends","The target player isn't found in friend list.");

            } else if (!Hack.friends().isFriend(args[1])) {
                ChatUtils.info("Friends",args[1] + " The target player isn't found in friend list.");
            } else {
                ChatUtils.info("Friends",args[1] + " is removed from friend list.");
                Hack.friends().friends().remove(args[1]);
            }
        } else {
            sendClientMessage("Invalid input! Usage: " + getPrefix() + "f <add/del> <name>");
        }

    }

}
