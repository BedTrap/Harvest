package me.eureka.harvest.systems.modules.render.Trajectories;

import me.eureka.harvest.events.event.Render3DEvent;
import me.eureka.harvest.events.event.TickEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.ic.util.Renderer3D;
import me.eureka.harvest.mixininterface.Vec3;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.modules.client.Rotations.Pool;
import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Module.Info(name = "Trajectories", category = Module.Category.Render)
public class Trajectories extends Module {
    public Setting<Boolean> pearls = super.register("Pearls", true);
    public Setting<Boolean> accurate = super.register("Accurate", true);
    public Setting<Integer> simulationSteps = super.register("Steps", 500, 0, 5000);

    private final ProjectileEntitySimulator simulator = new ProjectileEntitySimulator();
    private final Pool<Vec3> vec3s = new Pool<>(Vec3::new);
    private final List<Path> paths = new ArrayList<>();

    public final List<Trail> trails = new ArrayList<>();

    @Subscribe
    public void onTick(TickEvent.Post event) {
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof PersistentProjectileEntity arrow)) continue;

            if (added(arrow.getId())) continue;
            trails.add(new Trail(arrow.getId()));
        }

        if (trails.isEmpty()) return;
        trails.removeIf(trail -> {
            long age = System.currentTimeMillis() - trail.time;
            return age > 4000;
        });
    }

    @Subscribe
    public void onRender(Render3DEvent event) {
        if (!trails.isEmpty()) {
            for (Trail trail : trails) {
                Entity arrow = mc.world.getEntityById(trail.id);
                if (arrow == null) continue;

                Vec3d vec3d = arrow.getPos();
                trail.vec3d.add(new Vec3d(vec3d.x, vec3d.y - 0.15, vec3d.z));

                int size = trail.vec3d.size();
                for (int i = 0; i < size; i++) {
                    Vec3d v1 = trail.vec3d.get(i);
                    Vec3d v2 = null;

                    try {
                        v2 = trail.vec3d.get(i + 1);
                    } catch (Exception ignored) {
                    }

                    render(event, v1, v2);
                }
            }
        }

        if (pearls.get()) {
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof EnderPearlEntity) {
                    calculatePath(entity, event.partial());
                    for (Path path : paths) path.render(event);
                }
            }
        }
    }

    private void render(Render3DEvent event, Vec3d v1, Vec3d v2) {
        if (v1 == null || v2 == null) return;
        v1 = Renderer3D.getRenderPosition(v1);
        v2 = Renderer3D.getRenderPosition(v2);

        Renderer3D.drawLine(event.matrix(), (float) v1.x, (float) v1.y, (float) v1.z, (float) v2.x, (float) v2.y, (float) v2.z, Color.WHITE.getRGB());
    }

    private Path getEmptyPath() {
        for (Path path : paths) {
            if (path.points.isEmpty()) return path;
        }

        Path path = new Path();
        paths.add(path);
        return path;
    }

    private void calculatePath(Entity entity, double tickDelta) {
        for (Path path : paths) path.clear();

        // Calculate paths
        simulator.set(entity, 1.5, 0.03, 0.8, accurate.get(), tickDelta);;
        getEmptyPath().calculate();
    }

    public Trail trailById(int id) {
        for (Trail trail : trails) {
            if (trail.id == id) return trail;
        }

        return null;
    }

    public boolean added(int id) {
        return trailById(id) != null;
    }

    public static Trajectories INSTANCE;

    public Trajectories() {
        INSTANCE = this;
    }

    public static class Trail {
        private final int id;
        private final List<Vec3d> vec3d = new ArrayList<>();
        private final long time;

        public Trail(int id) {
            this.id = id;
            this.time = System.currentTimeMillis();
        }
    }

    private class Path {
        private final List<Vec3> points = new ArrayList<>();

        private boolean hitQuad, hitQuadHorizontal;
        private double hitQuadX1, hitQuadY1, hitQuadZ1, hitQuadX2, hitQuadY2, hitQuadZ2;

        private Entity entity;

        public void clear() {
            for (Vec3 point : points) vec3s.free(point);
            points.clear();

            hitQuad = false;
            entity = null;
        }

        public void calculate() {
            addPoint();

            for (int i = 0; i < INSTANCE.simulationSteps.get(); i++) {
                HitResult result = simulator.tick();

                if (result != null) {
                    processHitResult(result);
                    break;
                }

                addPoint();
            }
        }

        private void addPoint() {
            points.add(vec3s.get().set(simulator.pos));
        }

        private void processHitResult(HitResult result) {
            if (result.getType() == HitResult.Type.BLOCK) {
                BlockHitResult res = (BlockHitResult) result;

                hitQuad = true;
                hitQuadX1 = res.getPos().x;
                hitQuadY1 = res.getPos().y;
                hitQuadZ1 = res.getPos().z;
                hitQuadX2 = res.getPos().x;
                hitQuadY2 = res.getPos().y;
                hitQuadZ2 = res.getPos().z;

                if (res.getSide() == Direction.UP || res.getSide() == Direction.DOWN) {
                    hitQuadHorizontal = true;
                    hitQuadX1 -= 0.25;
                    hitQuadZ1 -= 0.25;
                    hitQuadX2 += 0.25;
                    hitQuadZ2 += 0.25;
                } else if (res.getSide() == Direction.NORTH || res.getSide() == Direction.SOUTH) {
                    hitQuadHorizontal = false;
                    hitQuadX1 -= 0.25;
                    hitQuadY1 -= 0.25;
                    hitQuadX2 += 0.25;
                    hitQuadY2 += 0.25;
                } else {
                    hitQuadHorizontal = false;
                    hitQuadZ1 -= 0.25;
                    hitQuadY1 -= 0.25;
                    hitQuadZ2 += 0.25;
                    hitQuadY2 += 0.25;
                }

                points.add(vec3s.get().set(result.getPos()));
            } else if (result.getType() == HitResult.Type.ENTITY) {
                entity = ((EntityHitResult) result).getEntity();

                points.add(vec3s.get().set(result.getPos()).add(0, entity.getHeight() / 2, 0));
            }
        }

        public void render(Render3DEvent event) {
            // Render path
            Vec3 lastPoint = null;

            for (Vec3 point : points) {
                if (lastPoint != null) {
                    INSTANCE.render(event, new Vec3d(lastPoint.x, lastPoint.y, lastPoint.z), new Vec3d(point.x, point.y, point.z));
                }
                lastPoint = point;
            }

            // Render hit quad
            if (hitQuad) {
                Vec3d rPos = Renderer3D.getRenderPosition(hitQuadX1, hitQuadY1, hitQuadZ1);
                Box box = new Box(rPos.x - 0.2, rPos.y - 0.2, rPos.z - 0.2, rPos.x + 0.2, rPos.y + 0.2, rPos.z + 0.2);
                Renderer3D.get.drawBoxWithOutline(event.matrix(), box);
            }

            // Render entity
//            if (entity != null) {
//                double x = (entity.getX() - entity.prevX) * event.tickDelta;
//                double y = (entity.getY() - entity.prevY) * event.tickDelta;
//                double z = (entity.getZ() - entity.prevZ) * event.tickDelta;
//
//                Box box = entity.getBoundingBox();
//                event.renderer.box(x + box.minX, y + box.minY, z + box.minZ, x + box.maxX, y + box.maxY, z + box.maxZ, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
//            }
        }
    }
}
