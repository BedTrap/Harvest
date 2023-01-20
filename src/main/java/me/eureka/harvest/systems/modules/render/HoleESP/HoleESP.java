package me.eureka.harvest.systems.modules.render.HoleESP;

import me.eureka.harvest.events.event.Render3DEvent;
import me.eureka.harvest.events.event.TickEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.utils.advanced.BlockPosX;
import com.google.common.eventbus.Subscribe;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

@Module.Info(name = "HoleESP", category = Module.Category.Render)
public class HoleESP extends Module {
    public Setting<String> mode = register("Mode", List.of("Line", "Fill", "Both"), "Both");
    public Setting<Double> height = register("Height", 1D, 0.1D, 1.5D, 2);
    public Setting<Boolean> ignoreOwn = register("IgnoreOwn", true);
    public Setting<Integer> alpha = register("Alpha", 140, 0, 255);

    private final List<Hole> holeList = new ArrayList<>();

    @Subscribe
    public void onTick(TickEvent.Post event) {
        BlockPosX centerPos = new BlockPosX(mc.player.getBlockPos());
        holeList.clear();

        for (int i = centerPos.x() - 7; i < centerPos.x() + 7; i++) {
            for (int j = centerPos.y() - 4; j < centerPos.y() + 4; j++) {
                for (int k = centerPos.z() - 7; k < centerPos.z() + 7; k++) {
                    BlockPosX pos = new BlockPosX(i, j, k);

                    if (!allowed(pos, ignoreOwn.get())) continue;

                    int safe = 0;
                    int unsafe = 0;

                    for (Direction direction : Direction.values()) {
                        if (direction == Direction.UP || direction == Direction.DOWN) continue;

                        BlockState state = pos.offset(direction).state();

                        if (state.getBlock() == Blocks.BEDROCK) safe++;
                        else if (state.getBlock() == Blocks.OBSIDIAN) unsafe++;
                    }
                    if (safe + unsafe != 4) continue;

                    if (safe == 4) {
                        holeList.add(new Hole(pos.get(), Hole.HoleType.Safe));
                    } else holeList.add(new Hole(pos.get(), Hole.HoleType.Unsafe));
                }
            }
        }
    }

    private boolean allowed(BlockPosX pos, boolean ignoreOwn) {
        if (!pos.air()) return false;
        if ((ignoreOwn && (mc.player.getBlockPos().equals(pos.get())))) return false;
        for (int h = 0; h < 4; h++) {
            if (!pos.up(h).air()) return false;
        }
        return !pos.down().air();
    }

    @Subscribe
    public void onRender(Render3DEvent event) {
        for (Hole hole : holeList) hole.render(event, height.get(), alpha.get());
    }

    public static HoleESP INSTANCE;

    public HoleESP() {
        INSTANCE = this;
    }
}
