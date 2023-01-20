package me.eureka.harvest.systems.modules.movement.Jesus;

import me.eureka.harvest.events.event.TickEvent;
import me.eureka.harvest.systems.modules.Module;
import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

@Module.Info(name = "Jesus", category = Module.Category.Movement)
public class Jesus extends Module {

    public static Jesus get;

    public Jesus() {
        get = this;
    }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        Entity player = mc.player.getRootVehicle();

        if (player.isSneaking() || player.fallDistance > 3f) return;

        if (isSubmerged(player.getPos().add(0, 0.3, 0))) {
            player.setVelocity(player.getVelocity().x, 0.08, player.getVelocity().z);
        } else if (isSubmerged(player.getPos().add(0, 0.1, 0))) {
            player.setVelocity(player.getVelocity().x, 0.05, player.getVelocity().z);
        } else if (isSubmerged(player.getPos().add(0, 0.05, 0))) {
            player.setVelocity(player.getVelocity().x, 0.01, player.getVelocity().z);
        } else if (isSubmerged(player.getPos())) {
            player.setVelocity(player.getVelocity().x, -0.005, player.getVelocity().z);
            player.setOnGround(true);
        }
    }

    private boolean isSubmerged(Vec3d pos) {
        BlockPos bp = new BlockPos(pos);
        FluidState state = mc.world.getFluidState(bp);

        return !state.isEmpty() && pos.y - bp.getY() <= state.getHeight();
    }
}