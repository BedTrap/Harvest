package me.eureka.harvest.systems.commands;

import me.eureka.harvest.ic.Command;
import org.reflections.Reflections;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;

public class Commands {
    private final ArrayList<Command> commands;
    private boolean commandFound = false;

    public Commands() {
        commands = new ArrayList<>();

        Set<Class<? extends Command>> reflections = new Reflections("me.eureka.harvest.systems.commands").getSubTypesOf(Command.class);
        reflections.forEach(aClass -> {
            try {
                commands.add(aClass.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        });

        commands.sort(Comparator.comparing(command -> command.getName()[0]));
    }

    public void runCommand(String input) {
        String[] argss = input.split(" ");
        String command = argss[0];
        String args = input.substring(command.length()).trim();

        commands.forEach(c -> {
            for (String name : c.getName()) {
                if (argss[0].equalsIgnoreCase(name)) {
                    c.onCommand(command, args.split(" "));
                    commandFound = true;
                }
            }
//            if (!commandFound) {
//                 Command.sendClientMessage(Formatting.GRAY + "Invalid input.");
//            }
        });
    }

    public ArrayList<Command> getCommands() {
        return commands;
    }
}
