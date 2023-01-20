package me.eureka.harvest.mixins;

import io.netty.channel.Channel;
import io.netty.util.concurrent.GenericFutureListener;
import me.eureka.harvest.Hack;
import me.eureka.harvest.events.event.PacketEvent;
import me.eureka.harvest.ic.Command;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.Future;

@Mixin(ClientConnection.class)
public class MixinClientConnection {

    @Shadow
    private Channel channel;
    @Shadow
    @Final
    private NetworkSide side;

    @Inject(method = "send(Lnet/minecraft/network/Packet;)V", at = @At("HEAD"), cancellable = true)
    public void send(Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof ChatMessageC2SPacket pack) {
            if (pack.chatMessage().startsWith(Command.getPrefix())) {
                Hack.commands().runCommand(pack.chatMessage().substring(Command.getPrefix().length()));
                ci.cancel();
            }
        }
    }

    @Inject(method = "handlePacket", at = @At("HEAD"), cancellable = true)
    private static <T extends PacketListener> void onHandlePacket(Packet<T> packet, PacketListener listener, CallbackInfo info) {
        PacketEvent.Receive event = new PacketEvent.Receive(packet);
        Hack.event().post(event);

        if (event.isCancelled()) info.cancel();
    }

    @Inject(at = @At("HEAD"), method = "send(Lnet/minecraft/network/Packet;)V", cancellable = true)
    private void onSendPacketHead(Packet<?> packet, CallbackInfo info) {
        PacketEvent.Send event = new PacketEvent.Send(packet);
        Hack.event().post(event);

        if (event.isCancelled()) info.cancel();
    }

    @Inject(method = "send(Lnet/minecraft/network/Packet;)V", at = @At("TAIL"), cancellable = true)
    private void onSendPacketTail(Packet<?> packet, CallbackInfo info) {
        PacketEvent.Send event = new PacketEvent.Send(packet);
        Hack.event().post(event);

        if (event.isCancelled()) info.cancel();
    }
}
