package me.eureka.harvest.ic;

import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.List;

public class Friends {
    private static List<String> friends;

    public Friends() {
        friends = new ArrayList<>();
    }

    public boolean isFriend(String name) {
        return friends.stream().anyMatch(f -> f.equalsIgnoreCase(name));
    }

    public boolean isFriend(PlayerEntity player) {
        return friends.stream().anyMatch(f -> f.equalsIgnoreCase(player.getGameProfile().getName()));
    }

    public List<String> friends() {
        return friends;
    }
}