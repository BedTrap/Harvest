package me.eureka.harvest.systems.modules.combat.BedAura;

import me.eureka.harvest.Hack;
import me.eureka.harvest.events.event.Render3DEvent;
import me.eureka.harvest.events.event.TickEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.ic.util.Draw3D;
import me.eureka.harvest.ic.util.Renderer3D;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.modules.client.Rotations.Rotations;
import me.eureka.harvest.systems.modules.combat.AutoCrystal.ACUtils;
import me.eureka.harvest.systems.modules.combat.AutoCrystal.Location;
import me.eureka.harvest.systems.modules.combat.FeetTrap.FTUtils;
import me.eureka.harvest.systems.modules.render.LogoutSpot.PlayerCopyEntity;
import me.eureka.harvest.systems.utils.advanced.BlockPosX;
import me.eureka.harvest.systems.managers.DamageManager;
import me.eureka.harvest.systems.utils.advanced.FindItemResult;
import me.eureka.harvest.systems.utils.advanced.InvUtils;
import me.eureka.harvest.systems.utils.other.TimerUtils;
import com.google.common.eventbus.Subscribe;
import net.minecraft.block.BedBlock;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BedItem;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static me.eureka.harvest.systems.modules.combat.AutoCrystal.ACUtils.move;
import static me.eureka.harvest.systems.modules.combat.BedAura.BAUtils.getSphere;

@Module.Info(name = "BedAura", category = Module.Category.Combat)
public class BedAura extends Module {
    public Setting<String> swap = super.register("Swap", List.of("Move", "Silent", "Normal", "OFF"), "Normal");
    public Setting<Boolean> inventory = super.register("Inventory", false);
    public Setting<Boolean> syncSlot = super.register("SyncSlot", true);

    public Setting<Double> placeRange = super.register("PlaceRange", 4.5, 0, 7, 2);
    public Setting<Double> placeRangeY = super.register("RangeY", 3, 0, 5, 2);
    public Setting<Integer> placeDelay = super.register("PlaceDelay", 10, 0, 20);
    public Setting<Boolean> airPlace = super.register("AirPlace", true);
    public Setting<Boolean> ignoreTerrain = super.register("IgnoreTerrain", true);

    public Setting<Double> minDmg = super.register("MinDMG", 7.5, 0, 36, 1);
    public Setting<Double> maxDmg = super.register("MaxDMG", 7.5, 0, 36, 1);
    public Setting<String> doPlace = super.register("Place", List.of("BestDMG", "MostDMG"), "BestDMG");

    // Predict
    public Setting<Boolean> prediction = register("Prediction", false);
    public Setting<Boolean> collision = register("Collision", false);
    public Setting<Double> offset = register("Offset", 0.50, 0, 3, 2);

    // Calculation
    public Setting<String> calculation = super.register("Calculation", List.of("Accurate", "Fast", "Semi"), "Fast");
    public Setting<Double> factor = super.register("Factor", 6, 1, 15, 2);
    public Setting<Boolean> automatic = super.register("Automatic", true);
    public Setting<Double> increase = super.register("Increase", 0, -1, 3, 2);
    public Setting<Boolean> debug = super.register("Debug", false);

    public Setting<String> render = super.register("Render", List.of("Box", "Smooth", "OFF"), "Box");
    public Setting<Integer> smoothFactor = super.register("SmoothFactor", 5, 2, 20);
    public Setting<String> shapeMode = register("ShapeMode", List.of("Line", "Fill", "Both"), "Both");
    public Setting<Integer> red = register("Red", 140, 0, 255);
    public Setting<Integer> green = register("Green", 140, 0, 255);
    public Setting<Integer> blue = register("Blue", 140, 0, 255);
    public Setting<Integer> alpha = register("Alpha", 140, 0, 255);
    public Setting<Boolean> sphere = super.register("Sphere", false);

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private BlockPosX bestPos;
    private double bestDamage;
    private Direction bestDirection;

    private BlockPosX renderPos;

    private List<BlockPosX> renderingSphere = new ArrayList<>();
    private final TimerUtils placeTimer = new TimerUtils();

    private final List<Location> currLocation = new ArrayList<>();
    private final List<Location> prevLocation = new ArrayList<>();

    // Predict
    @Subscribe
    private void onPre(TickEvent.Pre event) {
        prevLocation.clear();

        for (PlayerEntity player : mc.world.getPlayers()) {
            prevLocation.add(new Location(player));
        }
    }

    @Subscribe
    private void onPost(TickEvent.Post event) {
        currLocation.clear();

        for (PlayerEntity player : mc.world.getPlayers()) {
            currLocation.add(new Location(player));
        }
    }

    // Main code
    @Subscribe
    public void onTick(TickEvent.Post event) {
        if (mc.world.getDimension().bedWorks()) {
            info(this.name, "Beds can't be blew up here, toggling...");
            toggle(true);
            return;
        }

        executor.execute(this::doCalculate);
        doPlace();
    }

    private void doPlace() {
        doPlace(bestPos, bestDirection, false);
        setDisplayInfo(String.valueOf(getBestDamage()));
    }

    private void doPlace(BlockPosX bp, Direction direction, boolean ignoreDelay) {
        if (bp == null || direction == null || (!ignoreDelay && !placeTimer.passedTicks(placeDelay.get()))) return;

        FindItemResult bed = InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem);
        if ((!inventory.get() && !bed.isHotbar()) || !bed.found()) return;

        Hand hand = bed.isOffhand() ? Hand.OFF_HAND : Hand.MAIN_HAND;
        Direction placeDirection = !airPlace.get() && bp.hasNeighbours() ? bp.getNeighbour() : Direction.DOWN;
        BlockHitResult hitResult = new BlockHitResult(bestPos.closestVec3d(), placeDirection, bp.get(), false);

        if ((swap.get("Silent") || swap.get("Normal")) && !bed.isOffhand() && !bed.isMainHand()) {
            if (debug.get()) info(this.name, "Swapping to bed slot.");
            InvUtils.swap(bed);
        }

        boolean moveSwap = (swap.get("Move") && bed.isHotbar()) || (inventory.get()) && !bed.isHotbar();
        boolean holdsBed = (bed.isOffhand() || bed.isMainHand()) || moveSwap;
        if (holdsBed) {
            double yaw = getYaw(direction);
            double pitch = Rotations.instance.getPitch(bp.get());

            Rotations.instance.rotate(yaw, pitch, () -> {
                if (moveSwap) {
                    move(bed, () -> {
                        mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(hand, hitResult, 0));
                        mc.interactionManager.interactBlock(mc.player, Hand.OFF_HAND, hitResult);
                    });
                } else {
                    mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(hand, hitResult, 0));
                    mc.interactionManager.interactBlock(mc.player, Hand.OFF_HAND, hitResult);
                }
            });
            mc.player.swingHand(hand);
        }

        if (swap.get("Silent")) InvUtils.swapBack();
        if (syncSlot.get()) InvUtils.syncSlots();
        placeTimer.reset();
    }

    private void doCalculate() {
        long ms = System.currentTimeMillis();

        FindItemResult bed = InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem);
        if ((!inventory.get() && !bed.isHotbar()) || !bed.found()) return;

        List<BlockPosX> sphere = getSphere(mc.player, placeRange.get(), placeRangeY.get());
        List<PlayerEntity> players = getPlayers();

        if (!calculation.get("Accurate") && !players.isEmpty()) {
            sphere = sphere.stream().filter(bp -> someoneIntersects(players, bp, factor.get(), increase.get())).toList();
        }
        if (calculation.get("Semi") && sphere.isEmpty()) {
            sphere = getSphere(mc.player, placeRange.get(), placeRangeY.get());
        }
        sphere = sphere.stream().distinct().toList();
        renderingSphere = sphere;

        BlockPosX bestPos = null;
        double bestDamage = 0.0;
        Direction bestDirection = null;

        if (!players.isEmpty()) {
            try {
                for (BlockPosX bp : sphere) {
                    if (bp.distance() > placeRange.get()) continue;
                    if (!airPlace.get() && !bp.hasNeighbours()) continue;
                    if (bp.hasEntity()) continue;

                    Pair<Double, Direction> pair = getHighestDamage(bp);
                    if (pair.getLeft() > bestDamage) {
                        bestPos = bp;
                        bestDamage = pair.getLeft();
                        bestDirection = pair.getRight();
                    }
                }
            } catch (Exception e) {
                e.fillInStackTrace();
            }
        }

        this.bestPos = bestPos;
        this.renderPos = bestPos;
        this.bestDamage = bestDamage;
        this.bestDirection = bestDirection;
        if (debug.get()) info(name, "Calculated in " + (System.currentTimeMillis() - ms) + "ms.");
    }

    private List<PlayerEntity> getPlayers() {
        List<PlayerEntity> playerEntities = new ArrayList<>();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player.isCreative() || player.isSpectator()) continue;
            if (player instanceof PlayerCopyEntity) continue;
            if (player.distanceTo(mc.player) > 15) continue;
            if (Hack.friends().isFriend(player)) continue;
            if (player == mc.player) continue;
            if (player.isDead()) continue;

            playerEntities.add(player);
        }

        playerEntities.sort(Comparator.comparingDouble(mc.player::distanceTo));
        return playerEntities;
    }

    /**
     * @param factor   adds N range to the player
     * @param increase adds N range to the automatic mode
     * @return true if someone intersects with blockPos
     */
    private boolean someoneIntersects(List<PlayerEntity> list, BlockPosX bp, double factor, double increase) {
        for (PlayerEntity player : list) {
            double distance = mc.player.distanceTo(player);
            double searchFactor = 3;

            if (distance > 6) searchFactor = (distance - 3) + increase;
            if (!automatic.get()) searchFactor = factor;

            if (!airPlace.get() && !bp.hasNeighbours()) continue;
            if (bp.hasEntity()) continue; // ignore bed entity??
            if (!bp.replaceable()) continue;
            if (!mc.player.getBlockPos().isWithinDistance(bp, placeRange.get())) return false;
            if (player.getBlockPos().isWithinDistance(bp, searchFactor)) return true;
        }

        return false;
    }

    public List<BlockPosX> collisionBlocks(BlockPosX bp) {
        List<BlockPosX> array = new ArrayList<>();

        if (bp.blastRes()) array.add(bp);
        for (Direction direction : Direction.values()) {
            if (direction == Direction.UP || direction == Direction.DOWN) continue;
            BlockPosX pos = bp.offset(direction);
            if (pos.blastRes()) array.add(pos);
        }

        return array;
    }

    private Pair<Double, Direction> getHighestDamage(BlockPosX blockPosX) {
        Pair<Double, Direction> pair = new Pair<>(0.0, null);
        List<Damage> damages = new ArrayList<>();

        if (mc.world == null || mc.player == null) return pair;
        double bestDamage = 0;
        Direction bestDirection = null;

        for (PlayerEntity target : mc.world.getPlayers()) {
            if (target.isDead() || target.getHealth() == 0) continue;
            if (target instanceof PlayerCopyEntity) continue;
            if (Hack.friends().isFriend(target)) continue;
            if (target == mc.player) continue;

            double targetDamage = 0;
            Direction targetDirection;

            OtherClientPlayerEntity predictedTarget = null;
            if (prediction.get() && target.isAlive()) {
                predictedTarget = ACUtils.predictedTarget(target);
                double x = getPredict(target, offset.get())[0];
                double z = getPredict(target, offset.get())[1];
                predictedTarget.setPosition(x, target.getY(), z);
            }

            for (Direction direction : Direction.values()) {
                if (direction == Direction.UP || direction == Direction.DOWN) continue;
                PlayerEntity finalTarget = !prediction.get() ||
                        FTUtils.isSurrounded(target) ||
                        intersectsWithBlocks(target.getBoundingBox()) ||
                        collidesWithBlocks(predictedTarget) ? target : predictedTarget;

                BlockPosX bp = blockPosX.offset(direction);

                if (bp.replaceable() || bp.fluid() || bp.of(BedBlock.class)) {
                    double damage = DamageManager.bedDamage(finalTarget, bp.get(), ignoreTerrain.get());
                    if (damage < minDmg.get()) continue;
                    double selfDamage = DamageManager.bedDamage(mc.player, bp.get(), ignoreTerrain.get());
                    if (selfDamage > maxDmg.get()) continue;

                    damages.add(new Damage(damage, direction));
                }
            }
            if (damages.isEmpty()) continue;

            damages.sort(Comparator.comparingDouble(Damage::damage).reversed());
            targetDamage = damages.get(0).damage();
            targetDirection = damages.get(0).direction();

            if (targetDamage < minDmg.get()) continue;

            if (doPlace.get("BestDMG")) {
                if (targetDamage > bestDamage) {
                    bestDamage = targetDamage;
                    bestDirection = targetDirection;
                }
            } else {
                bestDamage += targetDamage;
                bestDirection = targetDirection;
            }
        }

        pair.setLeft(bestDamage);
        pair.setRight(bestDirection);
        return pair;
    }

    private boolean collidesWithBlocks(PlayerEntity player) {
        if (!collision.get()) return false;

        Box box = player.getBoundingBox();
        List<BlockPosX> list = collisionBlocks(new BlockPosX(box.getCenter()));
        for (BlockPos bp : list) {
            Box pos = new Box(bp);
            if (pos.intersects(box)) return true;
        }

        return false;
    }

    private boolean intersectsWithBlocks(Box box) {
        for (int x = (int) Math.floor(box.minX); x < Math.ceil(box.maxX); x++) {
            for (int y = (int) Math.floor(box.minY); y < Math.ceil(box.maxY); y++) {
                for (int z = (int) Math.floor(box.minZ); z < Math.ceil(box.maxZ); z++) {
                    BlockPosX bp = new BlockPosX(x, y, z);

                    if (!bp.replaceable()) return true;
                }
            }
        }

        return false;
    }

    private double[] getPredict(PlayerEntity player, double offset) {
        double x = player.getX(), z = player.getZ();

        if (currLocation.isEmpty() || prevLocation.isEmpty()) return new double[]{0, 0};
        Location cLoc = getLocation(player, currLocation);
        Location pLoc = getLocation(player, prevLocation);

        if (cLoc == null || pLoc == null) return new double[]{0, 0};
        double distance = cLoc.vec3d.distanceTo(pLoc.vec3d);

        if (cLoc.x > pLoc.x) {
            x += distance + offset;
        } else if (cLoc.x < pLoc.x) {
            x -= distance + offset;
        }

        if (cLoc.z > pLoc.z) {
            z += distance + offset;
        } else if (cLoc.z < pLoc.z) {
            z -= distance + offset;
        }

        return new double[]{x, z};
    }

    public Location getLocation(PlayerEntity player, List<Location> locations) {
        return locations.stream().filter(location -> location.player == player).findFirst().orElse(null);
    }

    private double getBestDamage() {
        return ((double) Math.round(bestDamage * 100) / 100);
    }

    private double getYaw(Direction direction) {
        return switch (direction) {
            case NORTH -> 180;
            case EAST -> -90;
            case WEST -> 90;
            case SOUTH -> 0;
            default -> throw new IllegalStateException("Unexpected value: " + direction);
        };
    }

    private Box smoothBox;

    @Subscribe
    public void onRender(Render3DEvent event) {
        if (renderPos != null && bestDirection != null) {
            Box box = getBox(renderPos, bestDirection);
            Color color = new Color(red.get(), green.get(), blue.get(), alpha.get());

            switch (render.get()) {
                case "Box" -> {
                    switch (shapeMode.get()) {
                        case "Line" -> Draw3D.drawOutlineBox(event, box, color);
                        case "Fill" -> Draw3D.drawFilledBox(event, box, color);
                        case "Both" -> Draw3D.drawBox(event, box, color);
                    }
                }
                case "Smooth" -> {
                    if (smoothBox == null) smoothBox = box;
                    double minxX = (box.minX - smoothBox.minX) / smoothFactor.get();
                    double minxY = (box.minY - smoothBox.minY) / smoothFactor.get();
                    double minxZ = (box.minZ - smoothBox.minZ) / smoothFactor.get();

                    double maxX = (box.maxX - smoothBox.maxX) / smoothFactor.get();
                    double maxY = (box.maxY - smoothBox.maxY) / smoothFactor.get();
                    double maxZ = (box.maxZ - smoothBox.maxZ) / smoothFactor.get();

                    smoothBox = new Box(smoothBox.minX + minxX, smoothBox.minY + minxY, smoothBox.minZ + minxZ, smoothBox.maxX + maxX, smoothBox.maxY + maxY, smoothBox.maxZ + maxZ);

                    switch (shapeMode.get()) {
                        case "Line" -> Draw3D.drawOutlineBox(event, smoothBox, color);
                        case "Fill" -> Draw3D.drawFilledBox(event, smoothBox, color);
                        case "Both" -> Draw3D.drawBox(event, smoothBox, color);
                    }
                }
            }
        }

        if (!sphere.get()) return;
        for (BlockPosX bp : renderingSphere) {
            Renderer3D.get.drawBoxWithOutline(event.matrix(), bp);
        }
    }

    private Box getBox(BlockPosX bp, Direction direction) {
        Vec3d v = Renderer3D.getRenderPosition(bp.get());
        return switch (direction) {
            case DOWN, UP -> null;
            case SOUTH -> new Box(v.x, v.y, v.z, v.x + 1, v.y + 0.6, v.z + 2);
            case NORTH -> new Box(v.x, v.y, v.z - 1, v.x + 1, v.y + 0.6, v.z + 1);
            case WEST -> new Box(v.x - 1, v.y, v.z, v.x + 1, v.y + 0.6, v.z + 1);
            case EAST -> new Box(v.x, v.y, v.z, v.x + 2, v.y + 0.6, v.z + 1);
        };
    }

    // Using in getHighestDamage()
    record Damage(double damage, Direction direction) {

    }
}