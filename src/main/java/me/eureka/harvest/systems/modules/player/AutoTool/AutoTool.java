package me.eureka.harvest.systems.modules.player.AutoTool;

import me.eureka.harvest.events.event.BreakBlockEvent;
import me.eureka.harvest.events.event.TickEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.mixininterface.IClientPlayerInteractionManager;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.utils.advanced.BlockPosX;
import me.eureka.harvest.systems.utils.advanced.FindItemResult;
import me.eureka.harvest.systems.utils.advanced.InvUtils;
import com.google.common.eventbus.Subscribe;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;

@Module.Info(name = "AutoTool", category = Module.Category.Player)
public class AutoTool extends Module {
    public Setting<Boolean> silent = super.register("Silent", true);

    private boolean mine;

    @Override
    public void onDeactivate() {
        mine = false;
    }

    @Subscribe
    private void onTick(TickEvent.Post event) {
        if (!mc.options.attackKey.isPressed()) return;

        if (mc.crosshairTarget.getPos() != null && mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            BlockPosX bp = new BlockPosX(((BlockHitResult) mc.crosshairTarget).getBlockPos());
            Direction direction = ((BlockHitResult) mc.crosshairTarget).getSide();

            if (bp.hardness() <= 0.0) {
                mine = true;
            } else if (shouldMineSpoof()) {
                FindItemResult tool = InvUtils.findFastestToolInHotbar(bp.state());

                if (!tool.found()) {
                    mine = true;
                } else if (mc.player.getInventory().selectedSlot != tool.slot()) {
                    mine = false;
                    int prevSlot = mc.player.getInventory().selectedSlot;

                    mc.player.getInventory().selectedSlot = tool.slot();
                    ((IClientPlayerInteractionManager) mc.interactionManager).syncSelected();
                    mc.player.swingHand(Hand.MAIN_HAND);
                    mc.interactionManager.updateBlockBreakingProgress(bp.get(), direction);
                    if (silent.get()) mc.player.getInventory().selectedSlot = prevSlot;
                    ((IClientPlayerInteractionManager) mc.interactionManager).syncSelected();
                }
            }
        }
    }

    @Subscribe
    private void onBreakBlock(BreakBlockEvent event) {
        BlockPosX bp = new BlockPosX(((BlockHitResult) mc.crosshairTarget).getBlockPos());
        FindItemResult tool = InvUtils.findFastestToolInHotbar(bp.state());

        if (!tool.found()) return;
        if (mc.player.getInventory().selectedSlot != tool.slot() && !mine && shouldMineSpoof()) {
            event.cancel();
        }
    }

    private boolean shouldMineSpoof() {
        return !mc.player.isCreative();
    }
}