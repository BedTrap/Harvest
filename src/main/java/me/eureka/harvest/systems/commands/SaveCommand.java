package me.eureka.harvest.systems.commands;

import me.eureka.harvest.ic.Command;
import me.eureka.harvest.ic.Configs;
import me.eureka.harvest.systems.utils.game.ChatUtils;

public class SaveCommand extends Command {

    public SaveCommand() {
        super(new String[]{"save"});
    }

    @Override
    public void onCommand(String name, String[] args) {
        try{
            Configs.get.start();
            ChatUtils.info("Config","Successfully saved.");
        } catch (Exception e){
            ChatUtils.info("Config","Exception while saving.");
        }
    }
}