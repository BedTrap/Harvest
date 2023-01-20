package me.eureka.harvest.systems.modules.combat.AnchorAura;

import me.eureka.harvest.Hack;
import me.eureka.harvest.ic.Friends;
import me.eureka.harvest.systems.modules.render.LogoutSpot.PlayerCopyEntity;
import me.eureka.harvest.systems.utils.advanced.BlockPosX;
import me.eureka.harvest.systems.utils.advanced.FindItemResult;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import me.eureka.harvest.ic.util.Wrapper;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AAUtils {
    public static List<BlockPosX> getSphere(PlayerEntity player, double radius) {
        Vec3d eyePos = new Vec3d(player.getX(), player.getY() + player.getEyeHeight(player.getPose()), player.getZ());
        List<BlockPosX> blocks = new ArrayList<>();

        for (double i = eyePos.getX() - radius; i < eyePos.getX() + radius; i++) {
            for (double j = eyePos.getY() - radius; j < eyePos.getY() + radius; j++) {
                for (double k = eyePos.getZ() - radius; k < eyePos.getZ() + radius; k++) {
                    BlockPosX blockPos = new BlockPosX(new BlockPos(i, j, k));

                    if (blockPos.distance() > radius) continue;
                    if (blocks.contains(blockPos)) continue;
                    blocks.add(blockPos);
                }
            }
        }

        return blocks.stream().filter(bp -> (bp.air() || bp.of(Blocks.RESPAWN_ANCHOR)) && !bp.hasEntity()).toList();
    }

    public static List<PlayerEntity> getPlayers() {
        List<PlayerEntity> playerEntities = new ArrayList<>();

        for (PlayerEntity player : Wrapper.mc.world.getPlayers()) {
            if (player.isCreative() || player.isSpectator()) continue;
            if (player instanceof PlayerCopyEntity) continue;
            if (player.distanceTo(Wrapper.mc.player) > 15) continue;
            if (Hack.friends().isFriend(player)) continue;
            if (player == Wrapper.mc.player) continue;
            if (player.isDead()) continue;

            playerEntities.add(player);
        }

        playerEntities.sort(Comparator.comparingDouble(Wrapper.mc.player::distanceTo));
        return playerEntities;
    }

    /**
     * @param increase adds N range to the automatic mode
     * @return true if someone intersects with blockPos
     */
    public static boolean someoneIntersects(List<PlayerEntity> list, BlockPosX bp, double placeRange, boolean airPlace, double increase) {
        for (PlayerEntity player : list) {
            double distance = Wrapper.mc.player.distanceTo(player);
            double searchFactor = 3;

            if (distance > 6) searchFactor = (distance - 3) + increase;

            if (!airPlace && !bp.hasNeighbours()) continue;
            if (bp.hasEntity()) continue;
            if (!Wrapper.mc.player.getBlockPos().isWithinDistance(bp, placeRange)) return false;
            if (player.getBlockPos().isWithinDistance(bp, searchFactor)) return true;
        }

        return false;
    }

    public static void doMove(FindItemResult itemResult, Runnable runnable) {
        if (itemResult.isOffhand()) {
            runnable.run();
            return;
        }

        doMove(Wrapper.mc.player.getInventory().selectedSlot, itemResult.slot());
        runnable.run();
        doMove(Wrapper.mc.player.getInventory().selectedSlot, itemResult.slot());
    }

    private static void doMove(int from, int to) {
        ScreenHandler handler = Wrapper.mc.player.currentScreenHandler;

        Int2ObjectArrayMap<ItemStack> stack = new Int2ObjectArrayMap<>();
        stack.put(to, handler.getSlot(to).getStack());

        Wrapper.mc.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(handler.syncId, handler.getRevision(), PlayerInventory.MAIN_SIZE + from, to, SlotActionType.SWAP, handler.getCursorStack().copy(), stack));
    }
}
