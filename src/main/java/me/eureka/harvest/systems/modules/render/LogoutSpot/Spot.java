package me.eureka.harvest.systems.modules.render.LogoutSpot;

import me.eureka.harvest.ic.util.Renderer3D;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static me.eureka.harvest.ic.util.Wrapper.mc;

public class Spot {
    private final List<Spot> spots = new ArrayList<>();
    public HashMap<String, PlayerCopyEntity> hashMap = new HashMap<>();

    private String name;
    private PlayerEntity player;
    private float hp;
    private int pops;
    private Vec3d textPos;

    public Spot() {

    }

    public Spot(String name, PlayerEntity player, float hp, int pops) {
        this.name = name;
        this.player = player;
        this.hp = hp;
        this.pops = pops;
        this.textPos = player.getPos().subtract(Renderer3D.getInterpolationOffset(player)).add(0, player.getHeight() + 0.50, 0);
    }

    public String getName() {
        return name;
    }
    public PlayerEntity getPlayer() {
        return player;
    }
    public int getPops() {
        return pops;
    }
    public float getHp() {
        return hp;
    }
    public Vec3d getTextPos() {
        return textPos;
    }
    public List<Spot> getSpots() {
        return spots;
    }

    public void add(String name, PlayerEntity player, float hp, int pops) {
        this.spots.add(new Spot(name, player, hp, pops));

        if (LogoutSpot.INSTANCE.render.get("Player")) {
            PlayerCopyEntity fakePlayer = new PlayerCopyEntity(player);
            fakePlayer.spawn();

            hashMap.put(name, fakePlayer);
        }
    }

    public void remove(Spot spot) {
        this.spots.remove(spot);
        if (LogoutSpot.INSTANCE.render.get("Player")) {
            hashMap.get(spot.name).remove(Entity.RemovalReason.DISCARDED);
            hashMap.remove(spot.name);
        }
    }

    public boolean online(PlayerListS2CPacket.Entry entry) {
        for (PlayerListEntry player : mc.getNetworkHandler().getPlayerList()) {
            if (player.getProfile().getId().equals(entry.getProfile().getId())) return true;
        }

        return false;
    }

    public PlayerEntity player(PlayerListS2CPacket.Entry entry) {
        return mc.world.getPlayerByUuid(entry.getProfile().getId());
    }

    public float entryHealth(PlayerListS2CPacket.Entry entry) {
        return player(entry).getHealth();
    }

    public boolean valid(PlayerListS2CPacket.Entry entry) {
        return this.player(entry) != null && !mc.player.equals(this.player(entry));
    }

    public boolean wasLogged(String name) {
        return getSpot(name) != null;
    }

    public Spot getSpot(String name) {
        for (Spot spot : getSpots()) {
            if (spot.player.getGameProfile().getName().equals(name)) return spot;
        }

        return null;
    }

    public boolean isLogged(String name) {
        for (Spot spot : getSpots()) {
            if (name.contains(spot.player.getGameProfile().getName())) return true;
        }

        return false;
    }
}

