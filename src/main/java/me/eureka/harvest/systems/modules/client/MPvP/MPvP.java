package me.eureka.harvest.systems.modules.client.MPvP;

import com.google.common.eventbus.Subscribe;
import me.eureka.harvest.events.event.EntityAddedEvent;
import me.eureka.harvest.mixins.ClientPlayerEntityAccessor;
import me.eureka.harvest.systems.modules.Module;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.message.DecoratedContents;
import net.minecraft.network.message.LastSeenMessageList;
import net.minecraft.network.message.MessageMetadata;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;

@Module.Info(name = "MPvP", category = Module.Category.Client)
public class MPvP extends Module {
    public String player;

    @Override
    public void onActivate() {
        player = null;
        info(this.name, "Please, type .mpvp name to start trollage.");
    }

    @Subscribe
    private void onAdded(EntityAddedEvent event) {
        if (player == null) return;

        if (!(event.entity instanceof PlayerEntity player)) return;
        if (player.getGameProfile().getName().equals(this.player)) teleport();
    }

    private void teleport() {
        if (mc.world.getDimension().bedWorks()) sendChatMessage("/spawn");
        else sendChatMessage("/overworld");
    }

    private void sendChatMessage(String string) {
        MessageMetadata metadata = MessageMetadata.of(mc.player.getUuid());
        DecoratedContents contents = new DecoratedContents(string);

        LastSeenMessageList.Acknowledgment acknowledgment = mc.getNetworkHandler().consumeAcknowledgment();
        MessageSignatureData messageSignatureData = ((ClientPlayerEntityAccessor) mc.player)._signChatMessage(metadata, contents, acknowledgment.lastSeen());
        mc.getNetworkHandler().sendPacket(new ChatMessageC2SPacket(contents.plain(), metadata.timestamp(), metadata.salt(), messageSignatureData, contents.isDecorated(), acknowledgment));
    }

    public static MPvP INSTANCE;

    public MPvP() {
        INSTANCE = this;
    }
}
