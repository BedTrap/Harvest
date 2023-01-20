package me.eureka.harvest.systems.modules.combat.BedAura;

import me.eureka.harvest.systems.utils.advanced.BlockPosX;
import me.eureka.harvest.systems.utils.advanced.FindItemResult;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.block.BedBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

import static me.eureka.harvest.ic.util.Wrapper.mc;

public class BAUtils {
    public static List<BlockPosX> getSphere(PlayerEntity player, double radius, double radiusY) {
        Vec3d eyePos = new Vec3d(player.getX(), player.getY() + player.getEyeHeight(player.getPose()), player.getZ());
        List<BlockPosX> blocks = new ArrayList<>();

        double range = radius;
        radius = Math.ceil(radius);
        radiusY = Math.ceil(radiusY);
        for (double i = eyePos.getX() - radius; i < eyePos.getX() + radius; i++) {
            for (double j = eyePos.getY() - radiusY; j < eyePos.getY() + radiusY; j++) {
                for (double k = eyePos.getZ() - radius; k < eyePos.getZ() + radius; k++) {
                    Vec3d vecPos = new Vec3d(i, j, k);
                    if (eyePos.distanceTo(vecPos) > radius) continue;

                    BlockPosX blockPos = new BlockPosX(vecPos);
                    if (range < blockPos.distance()) continue;

                    if (blocks.contains(blockPos)) continue;
                    blocks.add(blockPos);
                }
            }
        }

        return blocks.stream().filter(bp -> (bp.replaceable() || bp.fluid() || bp.of(BedBlock.class)) && !bp.hasEntity()).toList();
    }

    // Swap
    public static void move(FindItemResult itemResult, Runnable runnable) {
        if (itemResult.isOffhand()) {
            runnable.run();
            return;
        }

        move(mc.player.getInventory().selectedSlot, itemResult.slot());
        runnable.run();
        move(mc.player.getInventory().selectedSlot, itemResult.slot());
    }

    private static void move(int from, int to) {
        ScreenHandler handler = mc.player.currentScreenHandler;

        Int2ObjectArrayMap<ItemStack> stack = new Int2ObjectArrayMap<>();
        stack.put(to, handler.getSlot(to).getStack());

        mc.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(handler.syncId, handler.getRevision(), PlayerInventory.MAIN_SIZE + from, to, SlotActionType.SWAP, handler.getCursorStack().copy(), stack));
    }
}
