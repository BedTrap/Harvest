package me.eureka.harvest.systems.modules.combat.AutoCrystal;

import com.google.common.eventbus.Subscribe;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import me.eureka.harvest.Hack;
import me.eureka.harvest.events.event.*;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.ic.util.Draw3D;
import me.eureka.harvest.ic.util.Renderer3D;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.modules.client.HUD.HUD;
import me.eureka.harvest.systems.modules.client.Rotations.Rotations;
import me.eureka.harvest.systems.modules.combat.PistonCrystal.PistonCrystal;
import me.eureka.harvest.systems.modules.render.HitMarker.HitMarker;
import me.eureka.harvest.systems.modules.render.LogoutSpot.PlayerCopyEntity;
import me.eureka.harvest.systems.utils.advanced.BlockPosX;
import me.eureka.harvest.systems.managers.DamageManager;
import me.eureka.harvest.systems.utils.advanced.FindItemResult;
import me.eureka.harvest.systems.utils.advanced.InvUtils;
import me.eureka.harvest.systems.utils.game.ChatUtils;
import me.eureka.harvest.systems.utils.other.Threading;
import me.eureka.harvest.systems.utils.other.TimerUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.awt.*;
import java.util.*;
import java.util.List;

import static me.eureka.harvest.systems.modules.combat.AutoCrystal.ACUtils.*;

/**
 * @author Eureka
 */

// This is the best module I have ever wrote, but if you have troubles with AutoCrystal
// then try to configure it or eat grass bitch
@Module.Info(name = "AutoCrystal", category = Module.Category.Combat)
public class AutoCrystal extends Module {
    // General
    public Setting<String> swap = register("Swap", List.of("Move", "Silent", "Normal", "OFF"), "Normal");
    public Setting<Boolean> inventory = register("Inventory", false);
    public Setting<Boolean> refill = register("Refill", false);
    public Setting<Integer> swapDelay = register("SwapDelay", 0, 0, 20);
    public Setting<Boolean> syncSlot = register("SyncSlot", true);

    // Place&Break
    public Setting<Integer> placeDelay = register("PlaceDelay", 0, 0, 10);
    public Setting<Double> placeRange = register("PlaceRange", 4.7, 0, 7, 1);
    public Setting<Integer> breakDelay = register("BreakDelay", 0, 0, 10);
    public Setting<Double> breakRange = register("BreakRange", 4.7, 0, 7, 1);
    public Setting<Boolean> syncHit = register("SyncHit", true);
    public Setting<String> fastBreak = register("FastBreak", List.of("Kill", "Instant", "All", "OFF"), "OFF");
    public Setting<String> freqMode = register("FreqMode", List.of("EachTick", "Divide", "OFF"), "Divide");
    public Setting<Integer> frequency = register("Frequency", 20, 0, 20);
    public Setting<Integer> age = register("Age", 1, 0, 5);
    public Setting<Boolean> oldPlace = register("1.12", false);
    public Setting<Boolean> rotate = register("Rotate", false);
    public Setting<Boolean> rayTrace = register("RayTrace", false);
    public Setting<Boolean> ignoreTerrain = register("IgnoreTerrain", true);
    public Setting<String> attack = register("Attack", List.of("Packet", "Client", "Vanilla"), "Vanilla");
    public Setting<String> priority = register("Priority", List.of("Place", "Break"), "Break");

    // Prediction
    public Setting<Boolean> predict = register("Predict", false);
    public Setting<Boolean> collision = register("Collision", false);
    public Setting<Double> offset = register("Offset", 0.50, 0, 3, 2);
    public Setting<Boolean> predictID = register("PredictID", false);
    public Setting<Integer> delayID = register("DelayID", 0, 0, 5);

    // Damage
    public Setting<String> doPlace = register("Place", List.of("Best DMG", "Most DMG"), "Best DMG");
    public Setting<String> doBreak = register("Break", List.of("All", "PlacePos", "Best DMG"), "Best DMG");
    public Setting<Double> minDmg = register("MinDMG", 7.5, 0, 36, 1);
    public Setting<Double> maxDmg = register("MaxDMG", 7.5, 0, 36, 1);
    public Setting<Boolean> antiSelfPop = register("AntiSelfPop", true);
    public Setting<Boolean> antiFriendDamage = register("AntiFriendDamage", false);
    public Setting<Double> friendMaxDmg = register("FriendMaxDMG", 5.5, 0, 36, 1);

    // FacePlace
    public Setting<Boolean> facePlace = register("FacePlace", false);
    public Setting<Boolean> slowPlace = register("SlowPlace", false);
    public Setting<Double> faceHealth = register("FaceHealth", 11, 0, 36, 1);
    public Setting<Double> faceMinDMG = register("FaceMinDMG", 3, 0, 12, 1);
    public Setting<Integer> faceRadius = register("FaceRadius", 2, 1, 10);

    // Misc
    public Setting<Boolean> eatPause = register("EatPause", true);
    public Setting<Boolean> minePause = register("MinePause", false);

    // Calculation
    public Setting<String> calculation = register("Calculation", List.of("Accurate", "Fast", "Semi"), "Accurate");
    public Setting<Double> factor = register("Factor", 6, 1, 15, 2);
    public Setting<Boolean> automatic = register("Automatic", true);
    public Setting<Double> increase = register("Increase", 0, 0, 3, 2);
    public Setting<Boolean> debug = register("Debug", false);

    // Render
    public Setting<String> render = register("Render", List.of("Box", "Smooth", "None"), "Box");
    public Setting<Integer> smoothFactor = register("SmoothFactor", 2, 2, 20);
    public Setting<Integer> renderTime = register("RenderTime", 10, 0, 15);
    public Setting<Boolean> damage = register("Damage", true);
    public Setting<Boolean> tracer = register("Tracer", false);
    public Setting<String> shapeMode = register("ShapeMode", List.of("Line", "Fill", "Both"), "Both");
    public Setting<Integer> red = register("Red", 140, 0, 255);
    public Setting<Integer> green = register("Green", 140, 0, 255);
    public Setting<Integer> blue = register("Blue", 140, 0, 255);
    public Setting<Integer> alpha = register("Alpha", 140, 0, 255);
    public Setting<Boolean> sphere = register("Sphere", false);

    private BlockPosX bestPos;
    private BlockPosX renderPos;

    private int attacks;
    private int ticksPassed;
    private int renderTimer;
    private int lastEntityId;
    private int last;
    private int crystalSlot;

    private int tick;
    private int i;
    private int lastSpawned;

    private final int[] second = new int[20];
    private static int cps;

    private double bestDamage;

    private Box renderBox;

    private boolean facePlacing;

    private final List<EndCrystalEntity> crystals;

    private final IntSet brokenCrystals;
    private List<BlockPosX> renderingSphere;

    private final List<Location> currLocation;
    private final List<Location> prevLocation;

    private final TimerUtils placeTimer;
    private final TimerUtils breakTimer;
    private final TimerUtils swapTimer;
    private final TimerUtils idTimer;

    @Override
    public void onActivate() {
        bestPos = null;
        renderPos = null;

        renderTimer = renderTime.get();
        renderBox = null;

        tick = 0;
        Arrays.fill(second, 0);
        i = 0;

        facePlacing = false;

        brokenCrystals.clear();

        placeTimer.reset();
        swapTimer.reset();
        idTimer.reset();

        super.setDisplayInfo("0.0, 0");
    }

    @Override
    public void onDeactivate() {
        InvUtils.syncSlots();
    }

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


    @Subscribe
    public void onTick(TickEvent.Post event) {
        getCPS();
        getCrystals();

        if (renderTimer > 0 && renderPos != null) renderTimer--;

        if (ticksPassed >= 0) {
            ticksPassed--;
        } else {
            ticksPassed = 20;
            attacks = 0;
        }

        if (eatPause.get() && mc.player.isUsingItem() && (mc.player.getMainHandStack().isFood() || mc.player.getOffHandStack().isFood())) return;
        if (minePause.get() && mc.interactionManager.isBreakingBlock()) return;
        if (PistonCrystal.instance.shouldPause()) return;

        Threading.thread.execute(this::doCalculate);

        if (priority.get("Place")) {
            doPlace();
            doBreak();
        } else {
            doBreak();
            doPlace();
        }
    }

    @Subscribe
    public void onAdded(EntityAddedEvent event) {
        if (!(event.entity instanceof EndCrystalEntity)) return;

        if (fastBreak.get("Instant") || fastBreak.get("All")) {
            if (bestPos == null) return;
            BlockPosX entityPos = new BlockPosX(event.entity.getBlockPos().down());

            if (bestPos.equals(entityPos)) {
                doBreak(event.entity, false, false);
            }
        }

        last = event.entity.getId() - lastEntityId;
        lastEntityId = event.entity.getId();
    }

    @Subscribe
    public void onRemove(EntityRemovedEvent event) {
        if (!(event.entity instanceof EndCrystalEntity)) return;

        if (brokenCrystals.contains(event.entity.getId())) {
            lastSpawned = 20;
            tick++;

            removeId(event.entity);
        }
    }

    @Subscribe
    private void onSend(PacketEvent.Send event) {
        if (event.packet instanceof UpdateSelectedSlotC2SPacket) {
            swapTimer.reset();
        }
    }

    private void doPlace() {
        doPlace(bestPos);
    }

    private void doPlace(BlockPosX blockPos) {
        if (blockPos == null) bestDamage = 0;
        if (blockPos == null || !placeTimer.passedTicks(placeDelay.get())) return;

        FindItemResult crystal = InvUtils.find(Items.END_CRYSTAL);
        if (!crystal.found()) return;

        Hand hand = mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL ? Hand.OFF_HAND : Hand.MAIN_HAND;
        if (ACUtils.distanceTo(ACUtils.closestVec3d(blockPos.box())) > placeRange.get()) return;
        BlockHitResult hitResult = getResult(blockPos);

        if (!swap.get("OFF") && !swap.get("Move") && hand != Hand.OFF_HAND && !crystal.isMainHand())
            InvUtils.swap(crystal);

        boolean moveSwap = ((swap.get("Move") && crystal.isHotbar()) || inventory.get()) && hand != Hand.OFF_HAND;
        boolean holdsCrystal = (hand == Hand.OFF_HAND || crystal.isMainHand()) || moveSwap;
        if (holdsCrystal) {
            if (rotate.get()) Rotations.instance.rotate(blockPos);

            if (moveSwap) {
                move(crystal, () -> mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(hand, hitResult, 0)));
            } else mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(hand, hitResult, 0));
            mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(hand));
        }

        if (predictID.get()) {
            EndCrystalEntity endCrystal = new EndCrystalEntity(mc.world, blockPos.getX() + 0.5, blockPos.getY() + 1.0, blockPos.getZ() + 0.5);
            endCrystal.setShowBottom(false);

            if (idTimer.passedTicks(delayID.get())) {
                endCrystal.setId(lastEntityId + last);

                if (!isBadId(endCrystal.getId())) {
                    doBreak(endCrystal, false, true);
                    endCrystal.kill();
                    idTimer.reset();
                }
            }
        }

        if (swap.get("Silent")) InvUtils.swapBack();
        if (syncSlot.get()) InvUtils.syncSlots();

        placeTimer.reset();
        setRender(blockPos);
    }

    private void doBreak() {
        doBreak(getCrystal(), true, false);
    }

    private void doBreak(Entity entity, boolean checkAge, boolean predict) {
        if (entity == null || !breakTimer.passedTicks((slowPlace.get() && facePlacing) ? 10 : breakDelay.get()) || !swapTimer.passedTicks(swapDelay.get()) || !frequency() || (checkAge && entity.age < age.get())) return;

        if (distanceTo(closestVec3d(entity.getBoundingBox())) > breakRange.get()) return;
        if (syncHit.get()) mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.interact(entity, mc.player.isSneaking(), Hand.MAIN_HAND));

        ACUtils.processAttack(entity, attack.get(), closestVec3d(entity.getBoundingBox()));
        HitMarker.INSTANCE.post();

        if (!predict) {
            this.lastEntityId = 0;
        }

        if (fastBreak.get("Kill")) {
            entity.kill();

            lastSpawned = 20;
            tick++;
        }

        addBroken(entity);
        attacks++;
        breakTimer.reset();
    }

    private void doRefill(FindItemResult itemResult) {
        if (!refill.get()) return;

        if (itemResult.isHotbar()) {
            this.crystalSlot = itemResult.slot();
        } else if (!itemResult.isOffhand() && itemResult.found() && this.crystalSlot != -1) {
            refill(itemResult, this.crystalSlot);
        }
    }

    private BlockHitResult getResult(BlockPosX blockPos) {
        Vec3d eyesPos = new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());

        if (!rayTrace.get()) return new BlockHitResult(blockPos.closestVec3d(), blockPos.closestDirection(), blockPos, false);
        for (Direction direction : Direction.values()) {
            RaycastContext raycastContext = new RaycastContext(eyesPos, new Vec3d(blockPos.getX() + 0.5 + direction.getVector().getX() * 0.5,
                    blockPos.getY() + 0.5 + direction.getVector().getY() * 0.5,
                    blockPos.getZ() + 0.5 + direction.getVector().getZ() * 0.5), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
            BlockHitResult result = mc.world.raycast(raycastContext);
            if (result != null && result.getType() == HitResult.Type.BLOCK && result.getBlockPos().equals(blockPos)) {
                return result;
            }
        }

        return new BlockHitResult(blockPos.closestVec3d(), blockPos.getY() == 255 ? Direction.DOWN : Direction.UP, blockPos, false);
    }

    /*private void doSurroundBreak() {
        if (surroundBreak.get("OFF")) return;

        if (surroundBreak.get("OnMine") && !mc.interactionManager.isBreakingBlock()) return;
        List<BlockPosX> vulnerablePos = new ArrayList<>();

        try {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == mc.player) continue;
                if (Hack.friends().isFriend(player)) continue;
                if (!ACUtils.isSurrounded(player)) continue;

                for (BlockPosX bp : ACUtils.getSphere(player, 5)) {
                    if (ACUtils.hasEntity(bp.box(), entity -> entity == mc.player || entity == player || entity instanceof ItemEntity))
                        continue;

                    boolean canPlace = bp.up().air() && (bp.of(Blocks.OBSIDIAN) || bp.of(Blocks.BEDROCK));

                    if (!canPlace) continue;
                    Vec3d vec3d = new Vec3d(bp.getX(), bp.getY() + 1, bp.getZ());
                    Box endCrystal = new Box(vec3d.x - 0.5, vec3d.y, vec3d.z - 0.5, vec3d.x + 1.5, vec3d.y + 2, vec3d.z + 1.5);

                    for (BlockPosX surround : ACUtils.getSurroundBlocks(player, true)) {
                        if (surround.hardness() <= 0) return;

                        if (surroundBreak.get("OnMine") && mc.player.getMainHandStack().getItem() instanceof PickaxeItem) {
                            BlockPosX breakingPos = new BlockPosX(((ClientPlayerInteractionManagerAccessor) mc.interactionManager).breakingBlockPos());

                            if (!surround.equals(breakingPos)) continue;
                        }
                        Box box = surround.box();

                        if (endCrystal.intersects(box)) vulnerablePos.add(bp);
                    }
                }
            }
        } catch (Exception e) {
            e.fillInStackTrace();
        }

        if (vulnerablePos.isEmpty()) return;
        vulnerablePos.sort(Comparator.comparingDouble(ACUtils::distanceTo));
        BlockPosX blockPos = vulnerablePos.get(0);

        if (ACUtils.hasEntity(blockPos.up().box()) || ACUtils.distanceTo(ACUtils.closestVec3d(blockPos.box())) > placeRange.get())
            return;
        doPlace(blockPos);
    }*/

    private boolean someoneIntersects(List<PlayerEntity> list, BlockPosX blockPosX, double factor, double increase) {
        for (PlayerEntity player : list) {
            double distance = mc.player.distanceTo(player);
            double searchFactor = placeRange.get();

            if (distance > 6) searchFactor = (distance - 3) + increase;
            if (!automatic.get()) searchFactor = factor;

            if (ACUtils.hasEntity(blockPosX.box().stretch(0, oldPlace.get() ? 1 : 2, 0), entity -> !(entity instanceof EndCrystalEntity)))
                continue;

            if (!mc.player.getBlockPos().isWithinDistance(blockPosX, placeRange.get())) return false;
            if (player.getBlockPos().isWithinDistance(blockPosX, searchFactor)) return true;
        }

        return false;
    }

    private List<PlayerEntity> getPlayers() {
        List<PlayerEntity> playerEntities = new ArrayList<>();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player instanceof PlayerCopyEntity) continue;
            if (player.isCreative() || player.isSpectator()) continue;
            if (Hack.friends().isFriend(player)) continue;
            if (player == mc.player) continue;
            if (player.isDead()) continue;


            playerEntities.add(player);
        }

        playerEntities.sort(Comparator.comparingDouble(mc.player::distanceTo));
        return playerEntities;
    }

    private void doCalculate() {
        long ms = System.currentTimeMillis();
        HashMap<Double, BlockPosX> hashMap = new HashMap<>();
        List<BlockPosX> sphere = getSphere(mc.player, Math.ceil(placeRange.get()));
        List<PlayerEntity> players = getPlayers();

        if (!calculation.get("Accurate")) {
            if (!players.isEmpty()) {
                List<PlayerEntity> playersCopy = players;
                sphere = sphere.stream().filter(bp -> someoneIntersects(playersCopy, bp, factor.get(), increase.get())).toList();
            }
        }
        if (calculation.get("Semi")) {
            if (sphere.isEmpty()) sphere = getSphere(mc.player, Math.ceil(placeRange.get()));
        }
        renderingSphere = sphere;

        BlockPosX bestPos = null;
        double bestDamage = 0.0;

        if (!players.isEmpty() || !calculation.get("Fast")) {
            sphere = sphere.stream()
                    .filter(bp -> bp.up().air())
                    .filter(bp -> bp.of(Blocks.OBSIDIAN) || bp.of(Blocks.BEDROCK))
                    .filter(bp -> bp.distance() <= placeRange.get()).toList();

            try {
                for (BlockPosX bp : sphere) {
                    if (oldPlace.get() && !bp.up(2).air()) continue;

                    Vec3d crystal = bp.center().add(0, 0.5, 0);

                    double selfHealth = mc.player.getHealth() + mc.player.getAbsorptionAmount();

                    double targetDamage = getHighestDamage(crystal);
                    double selfDamage = DamageManager.crystalDamage(mc.player, crystal);

                    if (targetDamage <= minDmg.get()) continue;
                    if (selfDamage > maxDmg.get()) continue;
                    if (antiSelfPop.get() && selfDamage > selfHealth) continue;

                    if (canDamageFriends(crystal)) continue;
                    if (intersectsWithEntity(bp)) continue;

                    hashMap.put(targetDamage, bp);
                }
            } catch (Exception e) {
                e.fillInStackTrace();
            }
        }

        if (!hashMap.isEmpty()) {
            double key = hashMap.keySet().stream().max(Comparator.comparingDouble(Double::doubleValue)).get();

            bestPos = hashMap.get(key);
            bestDamage = key;
        }

        if (facePlace.get()) {
            if (bestPos == null) {
                players = players.stream().filter(player -> {
                    if (mc.player.distanceTo(player) > placeRange.get() + 3) return false;
                    if (player.getHealth() + player.getAbsorptionAmount() > faceHealth.get()) return false;

                    return isSurrounded(player);
                }).toList();

                if (!players.isEmpty()) {
                    for (PlayerEntity player : players) {
                        sphere = getSphere(new BlockPosX(player.getBlockPos().up()), faceRadius.get(), faceRadius.get());
                        sphere = sphere.stream().filter(bp -> {
                            if (bp.y() != player.getY()) return false;
                            if (bp.distance() > placeRange.get() - 1) return false;
                            if (intersectsWithEntity(bp)) return false;

                            return bp.up().air() && (bp.of(Blocks.OBSIDIAN) || bp.of(Blocks.BEDROCK));
                        }).toList();

                        if (!sphere.isEmpty()) {
                            for (BlockPosX bp : sphere) {
                                double targetDamage = DamageManager.crystalDamage(player, roundVec(bp));
                                if (targetDamage < faceMinDMG.get()) continue;

                                if (targetDamage > bestDamage) {
                                    bestPos = bp;

                                    bestDamage = targetDamage;
                                    facePlacing = true;
                                }
                            }

                            renderingSphere = sphere;
                        }
                    }
                }
            } else facePlacing = false;
        }

        this.bestPos = bestPos;
        this.bestDamage = bestDamage;
        if (debug.get()) ChatUtils.info(name, "Calculated in " + (System.currentTimeMillis() - ms) + "ms.");
    }

    private boolean intersectsWithEntity(BlockPosX bp) {
        if (facePlacing && hasCornerCrystal(bp)) return true;

        return ACUtils.hasEntity(bp.box().stretch(0, oldPlace.get() ? 1 : 2, 0), entity -> !(entity instanceof EndCrystalEntity));
    }

    private boolean hasCornerCrystal(BlockPosX bp) {
        if (bp.up().hasEntity(entity -> entity instanceof EndCrystalEntity)) {
            List<Entity> entities = bp.up().getEntities(entity -> entity instanceof EndCrystalEntity);
            entities = entities.stream().filter(entity -> bp.up().get().equals(entity.getBlockPos())).toList();
            return entities.isEmpty();
        }

        return false;
    }

    private boolean canDamageFriends(Vec3d vec3d) {
        if (!antiFriendDamage.get()) return false;

        for (PlayerEntity friend : mc.world.getPlayers()) {
            if (!Hack.friends().isFriend(friend)) continue;
            if (friend.isDead()) continue;

            double friendDamage = DamageManager.crystalDamage(friend, vec3d);
            if (friendDamage >= friendMaxDmg.get()) return true;
        }

        return false;
    }

    private double getHighestDamage(Vec3d vec3d) {
        if (mc.world == null || mc.player == null) return 0;
        double highestDamage = 0;

        for (PlayerEntity target : mc.world.getPlayers()) {
            if (target instanceof PlayerCopyEntity) continue;
            if (Hack.friends().isFriend(target)) continue;
            if (target == mc.player) continue;
            if (target.isDead() || target.getHealth() == 0) continue;

            double targetDamage = 0;
            boolean skipPredict = false;
            if (predict.get() && target.isAlive() && !intersects(target.getBoundingBox(), Blocks.COBWEB)) {
                if (!ACUtils.isSurrounded(target)) {
                    OtherClientPlayerEntity predictedTarget = predictedTarget(target);
                    double x = getPredict(target, offset.get())[0];
                    double z = getPredict(target, offset.get())[1];
                    predictedTarget.setPosition(x, target.getY(), z);

                    if (collision.get()) {
                        if (collidesWithBlocks(predictedTarget)) skipPredict = true;
                    }

                    targetDamage = skipPredict ? targetDamage : DamageManager.crystalDamage(predictedTarget, vec3d, null, ignoreTerrain.get());
                }
            }
            if (!predict.get() || skipPredict) targetDamage = DamageManager.crystalDamage(target, vec3d, null, ignoreTerrain.get());
            if (targetDamage < minDmg.get()) continue;
            if (doPlace.get("BestDMG")) {
                if (targetDamage > highestDamage) {
                    highestDamage = targetDamage;
                }
            } else highestDamage += targetDamage;
        }

        return highestDamage;
    }

    public boolean intersects(Box box, Block... blocks) {
        for (int x = (int) Math.floor(box.minX); x < Math.ceil(box.maxX); x++) {
            for (int y = (int) Math.floor(box.minY); y < Math.ceil(box.maxY); y++) {
                for (int z = (int) Math.floor(box.minZ); z < Math.ceil(box.maxZ); z++) {
                    BlockState state = mc.world.getBlockState(new BlockPos(x, y, z));

                    for (Block block : blocks) {
                        if (state.isOf(block)) return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isBadId(int id) {
        try {
            Entity entity = mc.world.getEntityById(id);
            return entity instanceof ItemEntity || entity instanceof ExperienceOrbEntity || entity instanceof PersistentProjectileEntity || entity == mc.player;
        } catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
            return false;
        }
    }

    public boolean collidesWithBlocks(PlayerEntity player) {
        Box box = player.getBoundingBox();
        List<BlockPosX> list = collisionBlocks(new BlockPosX(box.getCenter()));
        for (BlockPos bp : list) {
            Box pos = new Box(bp);
            if (pos.intersects(box)) return true;
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

    private void getCrystals() {
        crystals.clear();

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof EndCrystalEntity endCrystal)) continue;
            if (!endCrystal.isAlive()) continue;

            if (mc.player.distanceTo(endCrystal) > breakRange.get()) continue;
            //if (mc.player.distanceTo(endCrystal) > breakWallRange.get() && !mc.player.canSee(endCrystal)) continue;

            crystals.add(endCrystal);
        }
    }

    private EndCrystalEntity getCrystal() {
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof EndCrystalEntity)) continue;
            if (mc.player.distanceTo(entity) > breakRange.get()) continue;

            if (doBreak.get("All")) return (EndCrystalEntity) entity;

            double tempDamage = getHighestDamage(ACUtils.roundVec(entity));
            if (tempDamage > minDmg.get()) return (EndCrystalEntity) entity;
        }

        if (bestPos == null) return null;
        return ACUtils.getEntity(bestPos.up());
    }

    private double getBestDamage() {
        return ((double) Math.round(bestDamage * 100) / 100);
    }

    private boolean frequency() {
        switch (freqMode.get()) {
            case "EachTick" -> {
                if (attacks > frequency.get()) return false;
            }
            case "Divide" -> {
                if (!divide(frequency.get()).contains(ticksPassed)) return false;
            }
            case "OFF" -> {
                return true;
            }
        }

        return true;
    }

    public void getCPS() {
        i++;
        if (i >= second.length) i = 0;

        second[i] = tick;
        tick = 0;

        cps = 0;
        for (int i : second) cps += i;

        lastSpawned--;
        if (lastSpawned >= 0 && cps > 0) cps--;
        if (cps == 0) bestDamage = 0.0;

        super.setDisplayInfo(getBestDamage() + ", " + cps);
    }

    public ArrayList<Integer> divide(int frequency) {
        ArrayList<Integer> freqAttacks = new ArrayList<>();
        int size = 0;

        if (20 < frequency) return freqAttacks;
        else if (20 % frequency == 0) {
            for (int i = 0; i < frequency; i++) {
                size += 20 / frequency;
                freqAttacks.add(size);
            }
        } else {
            int zp = frequency - (20 % frequency);
            int pp = 20 / frequency;

            for (int i = 0; i < frequency; i++) {
                if (i >= zp) {
                    size += pp + 1;
                    freqAttacks.add(size);
                } else {
                    size += pp;
                    freqAttacks.add(size);
                }
            }
        }

        return freqAttacks;
    }

    private void addBroken(Entity entity) {
        if (!brokenCrystals.contains(entity.getId())) brokenCrystals.add(entity.getId());
    }

    private void removeId(Entity entity) {
        if (brokenCrystals.contains(entity.getId())) brokenCrystals.remove(entity.getId());
    }

    public Location getLocation(PlayerEntity player, List<Location> locations) {
        return locations.stream().filter(location -> location.player == player).findFirst().orElse(null);
    }

    public boolean shouldPause() {
        return !breakTimer.passedTicks(10);
    }

    public void setRender(BlockPosX blockPos) {
        renderPos = blockPos;
        renderTimer = renderTime.get();
    }

    @Subscribe
    public void onRender(Render3DEvent event) {
        // fixing AutoMine funny crash
        if (bestPos == null) return;
        if (render.get("None")) return;
        if (renderTimer == 0) renderPos = null;
        if (renderPos == null) return;

        // Smooth render (from 862 to 869 line)
        // Ricky is fucking bastard, he just copied this code and put into Meteor Client lmfao
        // If someone will get a question like
        //      <You posted this code after Ricky's PR>
        // Yeah but Ajaj leaked this src a long time ago :trolleybus:
        Box post = new Box(renderPos);
        if (renderBox == null) renderBox = post;

        double x = (post.minX - renderBox.minX) / smoothFactor.get();
        double y = (post.minY - renderBox.minY) / smoothFactor.get();
        double z = (post.minZ - renderBox.minZ) / smoothFactor.get();

        renderBox = new Box(renderBox.minX + x, renderBox.minY + y, renderBox.minZ + z, renderBox.maxX + x, renderBox.maxY + y, renderBox.maxZ + z);

        Vec3d realVec = render.get("Box") ? new Vec3d(renderPos.getX() + 0.5, renderPos.getY() + 0.5, renderPos.getZ() + 0.5) : renderBox.getCenter(); // Для рендера дамага
        Vec3d fixedVec = Renderer3D.getRenderPosition(render.get("Box") ? realVec : renderBox.getCenter());

        Box box = new Box(fixedVec.x - 0.5, fixedVec.y - 0.5, fixedVec.z - 0.5, fixedVec.x + 0.5, fixedVec.y + 0.5, fixedVec.z + 0.5);

        if (render.get("Box")) box = Box.from(Renderer3D.getRenderPosition(renderPos));
        Color color = new Color(red.get(), green.get(), blue.get(), alpha.get());

        switch (shapeMode.get()) {
            case "Line" -> Draw3D.drawOutlineBox(event, box, color);
            case "Fill" -> Draw3D.drawFilledBox(event, box, color);
            case "Both" -> Draw3D.drawBox(event, box, color);
        }

        if (damage.get()) {
            if (renderPos == null) return;

            Vec3d rPos = new Vec3d(realVec.getX(), realVec.getY(), realVec.getZ());
            Renderer3D.drawText(Text.of(String.valueOf(getBestDamage())), rPos.x, rPos.y, rPos.z, 0.8, false);
        }

        if (tracer.get()) {
            if (renderPos == null) return;

            renderTracer(event, fixedVec);
        }

        if (sphere.get()) {
            for (BlockPosX bp : renderingSphere) {
                switch (shapeMode.get()) {
                    case "Line" -> Draw3D.drawOutlineBox(event, bp.renderBox(), color);
                    case "Fill" -> Draw3D.drawFilledBox(event, bp.renderBox(), color);
                    case "Both" -> Draw3D.drawBox(event, bp.renderBox(), color);
                }
            }
        }
    }

    private void renderTracer(Render3DEvent event, Vec3d vec3d) {
        if (mc.crosshairTarget == null) return;
        Vec3d player = Renderer3D.getRenderPosition(mc.crosshairTarget.getPos());

        Renderer3D.drawLine(event.matrix(), (float) vec3d.x, (float) vec3d.y, (float) vec3d.z, (float) player.x, (float) player.y, (float) player.z, HUD.INSTANCE.colorType().getRGB());
    }

    public static AutoCrystal INSTANCE;

    public AutoCrystal() {
        INSTANCE = this;

        this.lastSpawned = 20;

        this.crystals = new ArrayList<>();
        this.brokenCrystals = new IntOpenHashSet();
        this.renderingSphere = new ArrayList<>();
        this.currLocation = new ArrayList<>();
        this.prevLocation = new ArrayList<>();
        this.placeTimer = new TimerUtils();
        this.breakTimer = new TimerUtils();
        this.swapTimer = new TimerUtils();
        this.idTimer = new TimerUtils();
    }

    {
        swap.setDescription("The way how items will be swapped into current slot.");
        swapDelay.setDescription("Swap delay.");
        syncSlot.setDescription("Prevents being desynced by using Silent swap. (Turn off on 1.12 servers)");
    }
}