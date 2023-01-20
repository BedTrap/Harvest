package me.eureka.harvest.systems.modules.world.AirPlace;

import me.eureka.harvest.events.event.TickEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.mixins.MinecraftClientAccessor;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.utils.advanced.BlockPosX;
import com.google.common.eventbus.Subscribe;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.SheepEntityRenderer;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;

import java.util.List;

@Module.Info(name = "AirPlace", category = Module.Category.World)
public class AirPlace extends Module {
    public Setting<String> mode = super.register("Mode", List.of("Normal", "Matrix"), "Normal");
    public Setting<Double> range = super.register("Range", 4.5, 0, 7, 2);

    @Subscribe
    public void onPost(TickEvent.Post event) {
        HitResult result = mc.getCameraEntity().raycast(range.get(), 0, false);
        if (!(result instanceof BlockHitResult hitResult) || !(mc.player.getMainHandStack().getItem() instanceof BlockItem))
            return;

        if (!mc.options.useKey.isPressed()) return;

        if (((MinecraftClientAccessor) mc).getItemUseCooldown() > 0) return;
        switch (mode.get()) {
            case "Normal" -> {
                mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hitResult, 0));
                mc.player.swingHand(Hand.MAIN_HAND);
            }
            case "Matrix" -> matrixPlace(hitResult);
        }

        ((MinecraftClientAccessor) mc).setItemUseCooldown(4);
    }

    private void matrixPlace(BlockHitResult hitResult) {
        BlockPosX bp = new BlockPosX(hitResult.getBlockPos());
        BlockPosX dbp = (BlockPosX) getPosition(bp)[0];
        Direction direction = (Direction) getPosition(bp)[1];

        // Matrix AirPlace bypass aka flagging pos
        // Using glowstone because this item will be always with you while you using AnchorAura
        // You are not able to use 2 the same blocks for this trick.
        int slot = getSlot();
        doMove(slot, () -> {
            BlockHitResult result = hitResult.withSide(direction);

            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result, 0));
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        });

        BlockHitResult result = hitResult.withSide(direction.getOpposite());
        mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result, 0));
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private int getSlot() {
        for (int i = 0; i < 8; i++) {
            if (i == mc.player.getInventory().selectedSlot) continue;
            ItemStack stack = mc.player.getInventory().getStack(i);

            if (stack.getItem() instanceof BlockItem) return i;
        }

        return -1;
    }

    private Object[] getPosition(BlockPosX bp) {
        double distance = 999.9;
        BlockPosX position = null;
        Direction direction = null;

        for (Direction dir : Direction.values()) {
            BlockPosX cbp = bp.offset(dir);
            if (cbp.hasEntity()) continue;

            double dis = cbp.distance();
            if (dis < distance) {
                distance = dis;
                position = cbp;
                direction = dir;
            }
        }

        return new Object[]{position, direction};
    }

    public void doMove(int slot, Runnable runnable) {
        if (slot == -1) {
            runnable.run();
            return;
        }

        doMove(mc.player.getInventory().selectedSlot, slot);
        runnable.run();
        doMove(mc.player.getInventory().selectedSlot, slot);
    }

    private void doMove(int from, int to) {
        ScreenHandler handler = mc.player.currentScreenHandler;

        Int2ObjectArrayMap<ItemStack> stack = new Int2ObjectArrayMap<>();
        stack.put(to, handler.getSlot(to).getStack());

        mc.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(handler.syncId, handler.getRevision(), PlayerInventory.MAIN_SIZE + from, to, SlotActionType.SWAP, handler.getCursorStack().copy(), stack));
    }
}
