package me.eureka.harvest.systems.modules.world.PacketMine;

import com.google.common.eventbus.Subscribe;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import me.eureka.harvest.Hack;
import me.eureka.harvest.events.event.Render3DEvent;
import me.eureka.harvest.events.event.StartBreakingBlockEvent;
import me.eureka.harvest.events.event.StateEvent;
import me.eureka.harvest.events.event.TickEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.ic.util.Renderer3D;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.modules.client.HUD.HUD;
import me.eureka.harvest.systems.modules.render.Nametags.Nametags;
import me.eureka.harvest.systems.modules.world.AutoMine.PMUtils;
import me.eureka.harvest.systems.utils.advanced.BlockPosX;
import me.eureka.harvest.systems.utils.advanced.FindItemResult;
import me.eureka.harvest.systems.utils.advanced.InvUtils;
import me.eureka.harvest.systems.utils.advanced.PacketUtils;
import me.eureka.harvest.systems.utils.other.Task;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.BarrierBlock;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Module.Info(name = "PacketMine", category = Module.Category.World)
public class PacketMine extends Module {
    public Setting<String> swap = register("Swap", List.of("Normal", "Silent", "Move", "OFF"), "Normal");
    public Setting<String> queue = register("Queue", List.of("Unlimited", "Limited"), "Unlimited");
    public Setting<Integer> size = register("Size", 5, 1, 25);

    public Setting<Boolean> speedMine = register("SpeedMine", false);
    public Setting<Double> rate = register("Rate", 1.0, 0.01, 1.0, 2);

    public Setting<Boolean> swing = register("Swing", false);
    public Setting<Boolean> render = register("Render", true);
    public Setting<String> color = register("Color", List.of("HUD", "Block", "Ready"), "Ready");
    public Setting<Integer> alpha = register("Alpha", 140, 0, 255);

    private final List<Queue> queues = new ArrayList<>();
    private final List<Queue> postQueue = new ArrayList<>();

    private final Task task = new Task();
    private final PacketUtils packets = new PacketUtils();

    @Override
    public void onActivate() {
        queues.clear();
        postQueue.clear();
        task.reset();
        packets.reset();
    }

    @Subscribe
    public void onStartBreaking(StartBreakingBlockEvent event) {
        BlockPosX bp = new BlockPosX(event.blockPos);
        if (bp.unbreakable()) {
            event.cancel();
            return;
        }

        if (!has(bp.key())) {
            if (queue.get("Limited") && queues.size() >= size.get()) {
                event.cancel();
                return;
            }

            queues.add(new Queue(bp));
        }

        event.cancel();
    }

    @Subscribe
    public void onPre(TickEvent.Pre event) {
        if (postQueue.isEmpty() || !speedMine.get()) return;

        FindItemResult tool = InvUtils.findFastestToolInHotbar(postQueue.get(0).bp().state());
        postQueue.get(0).progress += PMUtils.getBreakDelta(tool.found() ? tool.slot() : mc.player.getInventory().selectedSlot, postQueue.get(0).bp().state());
        if (postQueue.get(0).progress() >= 1.0) {
            if (tool.found() && !tool.isMainHand()) InvUtils.swap(tool);

            doSwing(swing.get());
            postQueue.remove(0);
        }
    }

    @Subscribe
    public void onPost(TickEvent.Post event) {
        if (queues.isEmpty()) return;

        // Remove from queue
        if (mc.crosshairTarget instanceof BlockHitResult hitResult) {
            BlockPosX bp = new BlockPosX(hitResult.getBlockPos());

            if (canRemove(bp)) {
                if (has(bp.key())) {
                    Queue queue = get(bp.key());

                    if (queue.progress() > 0.0) {
                        task.reset();
                        packets.reset();
                    }
                }

                queues.remove(get(bp.key()));
            }
        }

        // Removing bugged poses
        for (Queue queue : queues) {
            if (queue.bp().distance() > 4.6) {
                if (queue.progress() > 0.0) {
                    task.reset();
                    packets.reset();
                }

                queues.remove(queue);
            }
        }
        if (queues.isEmpty()) return;

        BlockPosX bp = queues.get(0).bp();
        packets.mine(bp, task, "Finish");

        queues.get(0).progress(packets.getProgress());

        boolean fast = speedMine.get() && packets.isReadyOn(rate.get()) && queues.size() > 1;
        if (packets.isReady() || fast) {
            FindItemResult tool = InvUtils.findFastestToolInHotbar(bp.state());
            int prevSlot = mc.player.getInventory().selectedSlot;

            if (!speedMine.get()) {
                switch (swap.get()) {
                    case "Normal", "Silent" -> {
                        if (tool.found() && !tool.isMainHand()) {
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, bp, Direction.UP));
                            InvUtils.swap(tool);
                        }
                    }
                    case "Move" -> move(tool, () -> {
                        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, bp, Direction.UP));
                        doSwing(swing.get());
                    });
                }
                if (!swap.get("Move")) {
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, bp, Direction.UP));
                    doSwing(swing.get());
                }
                if (swap.get("Silent")) InvUtils.swap(prevSlot);
            } else {
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, bp, Direction.UP));
                doSwing(false);
            }

            task.reset();
            packets.reset();

            if (speedMine.get()) postQueue.add(queues.get(0));
            queues.remove(0);
        }
    }

    @Subscribe
    public void onChange(StateEvent.Mode event) {
        if (event.module != this) return;
        if (!event.mode.equals("OFF")) return;

        postQueue.clear();
    }

    private boolean canRemove(BlockPosX bp) {
        if (!(mc.player.getMainHandStack().getItem() instanceof ToolItem)) return false;

        return mc.options.useKey.isPressed() && has(bp.key());
    }

    private void doSwing(boolean swing) {
        if (swing) {
            mc.player.swingHand(Hand.MAIN_HAND);
        } else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
    }

    public void move(FindItemResult itemResult, Runnable runnable) {
        if (itemResult.isOffhand()) {
            runnable.run();
            return;
        }

        move(mc.player.getInventory().selectedSlot, itemResult.slot());
        runnable.run();
        move(mc.player.getInventory().selectedSlot, itemResult.slot());
    }

    private void move(int from, int to) {
        ScreenHandler handler = mc.player.currentScreenHandler;

        Int2ObjectArrayMap<ItemStack> stack = new Int2ObjectArrayMap<>();
        stack.put(to, handler.getSlot(to).getStack());

        mc.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(handler.syncId, handler.getRevision(), PlayerInventory.MAIN_SIZE + from, to, SlotActionType.SWAP, handler.getCursorStack().copy(), stack));
    }

    public boolean has(String key) {
        return get(key) != null;
    }

    public Queue get(String key) {
        for (Queue queue : INSTANCE.queues) {
            if (Objects.equals(queue.key(), key)) return queue;
        }

        return null;
    }

    private Color getColor(BlockPosX bp) {
        switch (color.get()) {
            case "HUD" -> {
                return HUD.INSTANCE.colorType(alpha.get());
            }
            case "Block" -> {
                if (bp.of(Blocks.NETHER_PORTAL)) {
                    return new Color(107, 0, 209, alpha.get());
                }

                int color = bp.state().getMapColor(mc.world, bp.get()).color;
                return new Color(color & 0xff0000 >> 16, (color & 0xff00) >> 8, color & 0xff, alpha.get());
            }
            default -> {
                return null;
            }
        }
    }

    @Subscribe
    public void onRender(Render3DEvent event) {
        if (!render.get()) return;

        if (!queues.isEmpty()) {
            double progress = (!speedMine.get() || queues.size() == 1 ? 0.94 : rate.get());

            for (Queue queue : queues) {
                BlockPosX bp = queue.bp();

                Color color = getColor(bp);
                if (color == null) {
                    if (queue.progress() < progress) {
                        color = HUD.INSTANCE.color(Color.RED, alpha.get());
                    } else color = HUD.INSTANCE.color(Color.GREEN, alpha.get());
                }

                Vec3d vec3d = Renderer3D.getRenderPosition(bp.vec3d());
                Box box = new Box(vec3d.x, vec3d.y, vec3d.z, vec3d.x + 1, vec3d.y + 1, vec3d.z + 1);

                Renderer3D.get.drawBox(event.matrix(), box, color.getRGB());
                Renderer3D.get.drawOutlineBox(event.matrix(), box, color.getRGB());
            }
        }

        if (!postQueue.isEmpty()) {
            for (Queue queue : postQueue) {
                BlockPosX bp = queue.bp();

                Color color = getColor(bp);
                if (color == null) {
                    if (queue.progress() < 0.94) {
                        color = HUD.INSTANCE.color(Color.RED, alpha.get());
                    } else color = HUD.INSTANCE.color(Color.GREEN, alpha.get());
                }

                Vec3d vec3d = Renderer3D.getRenderPosition(bp.vec3d());
                Box box = new Box(vec3d.x, vec3d.y, vec3d.z, vec3d.x + 1, vec3d.y + 1, vec3d.z + 1);

                Renderer3D.get.drawBox(event.matrix(), box, color.getRGB());
                Renderer3D.get.drawOutlineBox(event.matrix(), box, color.getRGB());
            }
        }
    }

    public static PacketMine INSTANCE;

    public PacketMine() {
        INSTANCE = this;
    }

    public static class Queue {
        private BlockPosX bp;
        private double progress;

        private String key;

        public Queue(BlockPosX bp) {
            this.bp = bp;

            this.progress = 0.0;
            this.key = bp.x() + ", " + bp.y() + ", " + bp.z();
        }

        public BlockPosX bp() {
            return this.bp;
        }

        public double progress() {
            return this.progress;
        }

        public String key() {
            return this.key;
        }

        public void progress(double progress) {
            this.progress = progress;
        }
    }
}
