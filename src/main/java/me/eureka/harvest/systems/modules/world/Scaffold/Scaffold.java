package me.eureka.harvest.systems.modules.world.Scaffold;

import me.eureka.harvest.events.event.Render3DEvent;
import me.eureka.harvest.events.event.TickEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.ic.util.Renderer3D;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.modules.client.HUD.HUD;
import me.eureka.harvest.systems.utils.advanced.BlockPosX;
import me.eureka.harvest.systems.utils.advanced.FindItemResult;
import me.eureka.harvest.systems.utils.advanced.InvUtils;
import me.eureka.harvest.systems.utils.advanced.Place;
import me.eureka.harvest.systems.utils.other.TimerUtils;
import com.google.common.eventbus.Subscribe;
import me.eureka.harvest.systems.utils.game.ChatUtils;
import net.minecraft.item.BlockItem;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Module.Info(name = "Scaffold", category = Module.Category.World)
public class Scaffold extends Module {
    public Setting<String> swap = register("Swap", List.of("Normal", "Silent", "Move"), "Silent");
    public Setting<Integer> size = register("Size", 1, 0, 3);
    public Setting<Integer> actionDelay = register("ActionDelay", 5, 0, 500);
    public Setting<Integer> BPA = register("BPA", 1, 1, 5);
    public Setting<Boolean> packet = register("Packet", false);
    public Setting<Boolean> rotate = register("Rotate", false);
    public Setting<Boolean> preferHold = register("PreferHold", false);
    public Setting<Boolean> oldPlace = register("1.12", false);
    public Setting<Boolean> lockY = register("LockY", false);
    public Setting<Boolean> render = register("Render", true);
    public Setting<Integer> alpha = register("Alpha", 140, 0, 255);

    private int lockedY;

    private BlockPosX renderPos;
    private final TimerUtils timer = new TimerUtils();

    @Override
    public void onActivate() {
        lockedY = mc.player.getBlockPos().getY();
    }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        FindItemResult itemResult = getBlock(preferHold.get());
        if (!itemResult.found()) {
            ChatUtils.info("Can't find blocks. Disabling...");
            toggle();
            return;
        }

        List<BlockPosX> platform = getPlatform();
        if (platform.isEmpty()) {
            renderPos = null;
            return;
        }

        if (!timer.passedMillis(actionDelay.get())) return;
        int i = 0;
        for (BlockPosX bp : platform) {
            if (!mc.player.isOnGround() && bp.distance() > 4.6) continue;
            Place.place(bp, itemResult, Place.by(swap.get()), packet.get(), rotate.get(), oldPlace.get());
            renderPos = bp;

            i++;
            if (i >= BPA.get()) break;
        }

        timer.reset();
    }

    private List<BlockPosX> getPlatform() {
        List<BlockPosX> platform = new ArrayList<>();

        int x = mc.player.getBlockPos().getX();
        int y = (lockY.get() ? lockedY : mc.player.getBlockPos().getY()) - 1;
        int z = mc.player.getBlockPos().getZ();

        int x1 = x - size.get();
        int z1 = z - size.get();
        int x2 = x + size.get();
        int z2 = z + size.get();

        platform.add(new BlockPosX(x, y, z));
        BlockPos.iterate(new BlockPos(x1, y, z1), new BlockPos(x2, y, z2)).forEach(bp -> {
            BlockPosX bpx = new BlockPosX(bp);
            if (platform.contains(bpx)) return;

            platform.add(bpx);
        });

        if (oldPlace.get()) platform.sort(Comparator.comparing(BlockPosX::hasNeighbours).reversed());
        return platform.stream().filter(bp -> bp.air() && !bp.hasEntity()).toList();
    }

    public static FindItemResult getBlock(boolean preferHold) {
        return InvUtils.findInHotbar(itemStack -> {

            if (preferHold) {
                if (mc.player.getMainHandStack().getItem() instanceof BlockItem) return true;
                if (mc.player.getOffHandStack().getItem() instanceof BlockItem) return true;
            }

            return itemStack.getItem() instanceof BlockItem;
        });
    }

    @Subscribe
    public void onRender(Render3DEvent event) {
        if (!render.get()) return;
        if (renderPos == null) return;

        Renderer3D.get.drawBoxWithOutline(event.matrix(), renderPos.renderBox(), HUD.INSTANCE.colorType(alpha.get()));
    }

    {
        BPA.setDescription("Blocks per action.");
    }
}