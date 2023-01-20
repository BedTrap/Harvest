package me.eureka.harvest.systems.modules.combat.Burrow;

import me.eureka.harvest.events.event.TickEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.utils.advanced.BlockPosX;
import me.eureka.harvest.systems.utils.advanced.FindItemResult;
import me.eureka.harvest.systems.utils.advanced.InvUtils;
import com.google.common.eventbus.Subscribe;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.List;

@Module.Info(name = "Burrow", category = Module.Category.Combat)
public class Burrow extends Module {
    public Setting<String> mode = register("Mode", List.of("Strict", "Bypass"), "Strict");
    public Setting<String> block = register("Block", List.of("Anvil", "Obs", "Chest"), "Anvil");
    public Setting<Double> rubberband = register("RubberBand", 7, 1, 7, 1);

    private FindItemResult result;

    @Subscribe
    public void onTick(TickEvent.Post event) {
        BlockPosX blockPos = new BlockPosX(mc.player.getBlockPos());
        switch (block.get()) {
            case "Anvil" -> result = InvUtils.findInHotbar(Items.ANVIL);
            case "Obs" -> result = InvUtils.findInHotbar(Items.OBSIDIAN);
            case "Chest" -> result = InvUtils.findInHotbar(Items.ENDER_CHEST);
        }

        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.4, mc.player.getZ(), false));
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.75, mc.player.getZ(), false));
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.01, mc.player.getZ(), false));
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.15, mc.player.getZ(), false));

        InvUtils.swap(result.slot(), true);
        doPlace(blockPos);
        InvUtils.swapBack();

        switch (mode.get()) {
            case "Strict" -> mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + rubberband.get(), mc.player.getZ(), false));
            case "Bypass" -> mc.player.updatePosition(mc.player.getX(), mc.player.getY() + rubberband.get(), mc.player.getZ());
        }

        if (!blockPos.replaceable()) {
            info("Already burrowed, disabling...");
            toggle();
            return;
        }
        toggle();
    }

    public void doPlace(BlockPos blockPos) {
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(blockPos.getX(), blockPos.getY(), blockPos.getZ()), Direction.UP, blockPos, false));
        mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
    }
}