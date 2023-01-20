package me.eureka.harvest.systems.commands;

import me.eureka.harvest.ic.Command;
import me.eureka.harvest.ic.Configs;
import me.eureka.harvest.systems.utils.game.ChatUtils;

public class LoadCommand extends Command {

    public LoadCommand() {
        super(new String[]{"load"});
    }

    @Override
    public void onCommand(String name, String[] args) {
        try{
            Configs.get.load();
            ChatUtils.info("Config","Successfully loaded.");
        } catch (Exception e){
            ChatUtils.info("Config","Exception while loading.");
        }
    }
}
