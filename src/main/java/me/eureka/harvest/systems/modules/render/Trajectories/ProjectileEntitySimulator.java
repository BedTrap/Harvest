package me.eureka.harvest.systems.modules.render.Trajectories;

import me.eureka.harvest.mixininterface.IVec3d;
import me.eureka.harvest.mixininterface.Vec3;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.*;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;

import static me.eureka.harvest.ic.util.Wrapper.mc;

public class ProjectileEntitySimulator {
    private static final BlockPos.Mutable blockPos = new BlockPos.Mutable();

    private static final Vec3d pos3d = new Vec3d(0, 0, 0);
    private static final Vec3d prevPos3d = new Vec3d(0, 0, 0);

    public final Vec3 pos = new Vec3();
    private final Vec3 velocity = new Vec3();

    private double gravity;
    private double airDrag, waterDrag;

    public void set(Entity entity, double speed, double gravity, double waterDrag, boolean accurate, double tickDelta) {
        pos.set(entity, tickDelta);

        velocity.set(entity.getVelocity()).normalize().multiply(speed);

        if (accurate) {
            Vec3d vel = entity.getVelocity();
            velocity.add(vel.x, entity.isOnGround() ? 0.0D : vel.y, vel.z);
        }

        this.gravity = gravity;
        this.airDrag = 0.99;
        this.waterDrag = waterDrag;
    }

    public HitResult tick() {
        // Apply velocity
        ((IVec3d) prevPos3d).set(pos);
        pos.add(velocity);

        // Update velocity
        velocity.multiply(isTouchingWater() ? waterDrag : airDrag);
        velocity.subtract(0, gravity, 0);

        // Check if below 0
        if (pos.y < 0) return MissHitResult.INSTANCE;

        // Check if chunk is loaded
        int chunkX = (int) (pos.x / 16);
        int chunkZ = (int) (pos.z / 16);
        if (!mc.world.getChunkManager().isChunkLoaded(chunkX, chunkZ)) return MissHitResult.INSTANCE;

        // Check for collision
        ((IVec3d) pos3d).set(pos);
        HitResult hitResult = getCollision();

        return hitResult.getType() == HitResult.Type.MISS ? null : hitResult;
    }

    private boolean isTouchingWater() {
        blockPos.set(pos.x, pos.y, pos.z);

        FluidState fluidState = mc.world.getFluidState(blockPos);
        if (fluidState.getFluid() != Fluids.WATER && fluidState.getFluid() != Fluids.FLOWING_WATER) return false;

        return pos.y - (int) pos.y <= fluidState.getHeight();
    }

    private HitResult getCollision() {
        Vec3d vec3d3 = prevPos3d;

        HitResult hitResult = mc.world.raycast(new RaycastContext(vec3d3, pos3d, RaycastContext.ShapeType.COLLIDER, waterDrag == 0 ? RaycastContext.FluidHandling.ANY : RaycastContext.FluidHandling.NONE, mc.player));
        if (hitResult.getType() != HitResult.Type.MISS) {
            vec3d3 = hitResult.getPos();
        }

        HitResult hitResult2 = ProjectileUtil.getEntityCollision(mc.world, mc.player, vec3d3, pos3d, new Box(pos.x, pos.y, pos.z, pos.x, pos.y, pos.z).stretch(mc.player.getVelocity()).expand(1.0D), entity -> !entity.isSpectator() && entity.isAlive());
        if (hitResult2 != null) {
            hitResult = hitResult2;
        }

        return hitResult;
    }
}