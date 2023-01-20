package me.eureka.harvest.systems.modules.client.Rotations;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import static me.eureka.harvest.ic.util.Wrapper.mc;

public class Rotation {
    public double yaw, pitch;
    public int priority;
    public boolean clientSide;
    public Runnable callback;

    public void set(double yaw, double pitch, int priority, boolean clientSide, Runnable callback) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.priority = priority;
        this.clientSide = clientSide;
        this.callback = callback;
    }

    public void sendPacket() {
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround((float) yaw, (float) pitch, mc.player.isOnGround()));
        runCallback();
    }

    public void runCallback() {
        if (callback != null) callback.run();
    }
}