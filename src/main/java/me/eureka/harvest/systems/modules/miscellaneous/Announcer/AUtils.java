package me.eureka.harvest.systems.modules.miscellaneous.Announcer;

import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.world.GameMode;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import static me.eureka.harvest.ic.util.Wrapper.mc;

public class AUtils {
    public static boolean isOnline(UUID uuid) {
        if (mc.getNetworkHandler() == null) return false;
        for (PlayerListEntry player : mc.getNetworkHandler().getPlayerList()) {
            if (player.getProfile().getId().equals(uuid)) return true;
        }

        return false;
    }

    public static String getNameFromUUID(UUID uuid) {
        for (PlayerListEntry player : mc.getNetworkHandler().getPlayerList()) {
            if (player.getProfile().getId().equals(uuid)) return player.getProfile().getName();
        }

        return null;
    }

    public static GameMode getModeFromUUID(UUID uuid) {
        for (PlayerListEntry player : mc.getNetworkHandler().getPlayerList()) {
            if (player.getProfile().getId().equals(uuid)) return player.getGameMode();
        }

        return null;
    }

    public static String joinMessage(String name) {
        List<String> messages = List.of(
                "Hello " + name + ".",
                name + " is here.",
                "Good to see you, " + name,
                "Welcome, " + name,
                "Welcome to *server*, " + name,
                "Greetings, " + name,
                "Hey, " + name,
                "Good evening, " + name);

        Random random = new Random();
        return messages.get(random.nextInt(messages.size()));
    }

    public static String leaveMessage(String name) {
        List<String> messages = List.of(
                "See you later, " + name + ".",
                "Catch ya later, " + name + ".",
                "Bye, " + name,
                "Later, " + name,
                "See you next time, " + name,
                "Farewell, " + name);

        Random random = new Random();
        return messages.get(random.nextInt(messages.size()));
    }
}
