package me.eureka.harvest.systems.modules.render.HitMarker;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import io.netty.buffer.Unpooled;
import me.eureka.harvest.events.event.PacketEvent;
import me.eureka.harvest.events.event.RenderEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.utils.other.TimerUtils;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Identifier;

@Module.Info(name = "HitMarker", category = Module.Category.Render)
public class HitMarker extends Module {
    public Setting<Integer> lifeTime = register("LifeTime", 20, 1, 100);
    public Setting<Double> alpha = register("Alpha", 1.0, 0.0, 1.0, 2);

    private final Identifier hitmarker = new Identifier("picture", "hitmarker.png");
    private final TimerUtils timer = new TimerUtils();

    public void post() {
        timer.reset();
    }

    @Subscribe
    public void onPacker(PacketEvent.Send event) {
        if (!(event.packet instanceof PlayerInteractEntityC2SPacket packet)) return;
        if (type(packet) != InteractType.ATTACK) return;

        this.post();
    }

    @Subscribe
    public void onRender(RenderEvent event) {
        if (cantUpdate()) return;
        if (timer.passedMillis(lifeTime.get())) return;

        drawPicture(hitmarker);
    }

    private void drawPicture(Identifier identifier) {
        MatrixStack stacks = new MatrixStack();
        stacks.scale(1.0f, 1.0f, 1.0f);

        RenderSystem.setShaderTexture(0, identifier);
        RenderSystem.setShaderColor(1, 1, 1, alpha.get().floatValue());
        DrawableHelper.drawTexture(stacks, (mc.getWindow().getScaledWidth() / 2) - 9, (mc.getWindow().getScaledHeight() / 2) - 9, 0, 0, 17, 17, 17, 17);
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    public InteractType type(PlayerInteractEntityC2SPacket packet) {
        PacketByteBuf packetBuf = new PacketByteBuf(Unpooled.buffer());
        packet.write(packetBuf);

        packetBuf.readVarInt();
        return packetBuf.readEnumConstant(InteractType.class);
    }

    public enum InteractType {
        INTERACT,
        ATTACK,
        INTERACT_AT
    }

    public static HitMarker INSTANCE;

    public HitMarker() {
        INSTANCE = this;
    }
}
