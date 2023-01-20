package me.eureka.harvest.systems.modules.combat.PistonCrystal;

import me.eureka.harvest.events.event.EntityAddedEvent;
import me.eureka.harvest.events.event.EntityRemovedEvent;
import me.eureka.harvest.events.event.Render3DEvent;
import me.eureka.harvest.events.event.TickEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.ic.util.Renderer3D;
import me.eureka.harvest.ic.util.RotationUtil;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.modules.combat.PistonCrystal.positions.Positions;
import me.eureka.harvest.systems.modules.combat.PistonCrystal.positions.Redstone;
import me.eureka.harvest.systems.modules.combat.PistonCrystal.positions.Torch;
import me.eureka.harvest.systems.modules.render.HitMarker.HitMarker;
import me.eureka.harvest.systems.modules.world.Timer.Timer;
import me.eureka.harvest.systems.modules.client.HUD.HUD;
import me.eureka.harvest.systems.modules.client.Rotations.Rotations;
import me.eureka.harvest.systems.utils.advanced.FindItemResult;
import me.eureka.harvest.systems.utils.advanced.TargetUtils;
import me.eureka.harvest.systems.utils.other.TimerUtils;
import com.google.common.eventbus.Subscribe;
import net.minecraft.block.BlockState;
import net.minecraft.block.PistonBlock;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.List;

import static me.eureka.harvest.systems.utils.advanced.InvUtils.*;

@Module.Info(name = "PistonCrystal", category = Module.Category.Combat)
public class PistonCrystal extends Module {
    // General
    public Setting<Integer> targetRange = register("TargetRange", 7, 1, 10);
    public Setting<Double> placeRange = register("PlaceRange", 4.5, 1, 7, 1);
    public Setting<String> doBreak = register("Break", List.of("Packet", "Client"), "Packet");
    public Setting<String> distance = register("Distance", List.of("Closest", "Highest"), "Closest");
    public Setting<String> rotations = register("Rotations", List.of("Default", "Strict", "None"), "Default");

    // Delay
    public Setting<Integer> actionDelay = register("ActionDelay", 10, 0, 500);
    public Setting<Integer> placeTries = register("PlaceTries", 1, 1, 5);
    public Setting<Integer> delayedAttack = register("DelayedAttack", 25, 0, 100);
    public Setting<Integer> ticksExisted = register("TicksExisted", 0, 0, 7);
    public Setting<Integer> attackTries = register("AttackTries", 1, 1, 5);
    public Setting<Double> timer = register("Timer", 1, 1, 2, 2);

    // Placement
    public Setting<String> activator = register("Activator", List.of("Redstone", "Torch"), "Torch");
    public Setting<Boolean> strictDirection = register("StrictDirection", false);
    public Setting<Boolean> allowUpper = register("AllowUpper", true);
    public Setting<Boolean> trap = register("Trap", true);

    // Pause
    public Setting<Boolean> toggleOnJump = register("ToggleOnJump", true);
    public Setting<Boolean> pauseEat = register("PauseEat", true);
    public Setting<Boolean> pauseCA = register("PauseCA", true);

    // Render and Debug
    public Setting<Boolean> render = register("Render", true);
    public Setting<Boolean> debug = register("Debug", false);

    public PlayerEntity target;
    private BlockPos crystalPos;
    private BlockPos pistonPos;
    private BlockPos blockPos;
    public BlockPos trapPos;
    private Direction direction;

    private Box crystalBox = null;
    private Box pistonBox = null;
    private Box blockPosBox = null;

    private boolean shouldBreak;
    private EndCrystalEntity endCrystal;
    private int placed, attacks;

    private boolean rotated;
    private float[] prevRotation;

    private Stage stage;

    public Triplet currentTriplet;
    public Triplet stacked = null;
    private final TimerUtils timerUtils = new TimerUtils();

    @Override
    public void onActivate() {
        crystalPos = null;
        pistonPos = null;
        blockPos = null;
        trapPos = null;

        shouldBreak = true;
        endCrystal = null;
        placed = 0;
        attacks = 0;

        rotated = false;

        stage = Stage.Preparing;

        currentTriplet = null;
        stacked = null;
        timerUtils.reset();
    }

    @Override
    public void onDeactivate() {
        Timer.get.timerOverride(1.0F);
    }

    @Subscribe
    public void onAdded(EntityAddedEvent event) {
        if (!(event.entity instanceof EndCrystalEntity)) return;
        if (crystalPos == null) return;

        if (crystalPos.equals(event.entity.getBlockPos())) this.endCrystal = (EndCrystalEntity) event.entity;
    }

    @Subscribe
    public void onRemove(EntityRemovedEvent event) {
        if (this.endCrystal == null) return;

        if (event.entity.equals(this.endCrystal)) {
            this.endCrystal = null;
            event.entity.kill();
        }
    }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        target = TargetUtils.getPlayerTarget(targetRange.get(), TargetUtils.SortPriority.LowestDistance);
        if (TargetUtils.isBadTarget(target, targetRange.get())) {
            toggle();
            return;
        }

        if (pauseEat.get() && mc.player.isUsingItem() && (mc.player.getMainHandStack().isFood() || mc.player.getOffHandStack().isFood()))
            return;
        if (toggleOnJump.get() && ((mc.options.jumpKey.isPressed() || mc.player.input.jumping) || mc.player.prevY < mc.player.getPos().getY())) {
            toggle();
            return;
        }

        doCheck();
        switch (stage) {
            case Preparing -> {
                if (getPositions(target).direction == Direction.UP) {
                    stacked = getPositions(target);
                    return;
                }

                currentTriplet = getPositions(target);
                crystalPos = getPositions(target).blockPos.get(0);
                direction = getPositions(target).direction;

                pistonPos = getPositions(target).blockPos.get(1);
                blockPos = getPositions(target).blockPos.get(2);
                trapPos = target.getBlockPos().up(2);

                if (trap.get() && canPlace(trapPos, false)) doPlace(findInHotbar(Items.OBSIDIAN), trapPos);

                stacked = null;
                stage = Stage.Piston;
            }
            case Piston -> {
                if (!timerUtils.passedMillis(actionDelay.get())) return;

                doPlace(findInHotbar(Items.PISTON, Items.STICKY_PISTON), pistonPos);
                nextStage(Stage.Crystal);
            }
            case Crystal -> {
                if (!timerUtils.passedMillis(actionDelay.get())) return;

                if (!PCUtils.hasEntity(new Box(crystalPos), entity -> entity instanceof EndCrystalEntity)) {
                    if (placed <= placeTries.get()) {
                        doPlace(findInHotbar(Items.END_CRYSTAL), crystalPos.down());
                        placed++;

                        debug("PlaceTries: " + placed + ";");
                    }
                } else nextStage(Stage.Block);
            }
            case Block -> {
                if (!timerUtils.passedMillis(actionDelay.get())) return;

                doPlace(activator.get("Torch") ? findInHotbar(Items.REDSTONE_TORCH) : findInHotbar(Items.REDSTONE_BLOCK), blockPos);
                nextStage(Stage.Attack);
            }
            case Attack -> {
                if (doBreak.get("Packet") && activator.get("Redstone")) doBreak(blockPos);
                if (!timerUtils.passedMillis(actionDelay.get() + delayedAttack.get())) return;
                if (endCrystal != null && endCrystal.age <= ticksExisted.get()) return;

                if (this.endCrystal != null) {
                    if (attacks <= attackTries.get()) {
                        doAttack(endCrystal);
                        attacks++;

                        debug("AttackTries: " + attacks + ";");
                    }
                } else nextStage(Stage.BreakBlock);
            }
            case BreakBlock -> {
                if (rotated) {
                    mc.player.setYaw(prevRotation[0]);
                    mc.player.setPitch(prevRotation[1]);
                    rotated = false;
                }

                if (doBreak.get("Client") || activator.get("Torch")) doBreak(blockPos);
                if (this.endCrystal == null) swap(findFastestToolInHotbar(mc.world.getBlockState(blockPos)));
                mc.player.swingHand(Hand.MAIN_HAND);

                if (canPlace(blockPos, true)) {
                    shouldBreak = true;

                    if (Positions.isUpper()) nextStage(Stage.BreakPiston);
                    else nextStage(Stage.Preparing);
                }
            }
            case BreakPiston -> {
                doBreak(pistonPos);
                swap(findFastestToolInHotbar(mc.world.getBlockState(pistonPos)));
                mc.player.swingHand(Hand.MAIN_HAND);

                if (canPlace(pistonPos, false)) {
                    shouldBreak = true;

                    nextStage(Stage.Preparing);
                }
            }
        }

        setDisplayInfo(stage.name());
    }

    private void nextStage(Stage stage) {
        this.stage = stage;
        timerUtils.reset();

        placed = 0;
        attacks = 0;
    }

    private void doPlace(FindItemResult itemResult, BlockPos blockPos) {
        if (blockPos == null) return;
        if (!itemResult.found()) return;
        Hand hand = itemResult.isOffhand() ? Hand.OFF_HAND : Hand.MAIN_HAND;
        boolean isTorch = itemResult.equals(findInHotbar(Items.REDSTONE_TORCH));

        if (!itemResult.isOffhand()) mc.player.getInventory().selectedSlot = itemResult.slot();

        doRotate(() -> {
            mc.interactionManager.interactBlock(mc.player, hand, new BlockHitResult(PCUtils.closestVec3d(blockPos), Direction.DOWN, blockPos, false));
            mc.player.swingHand(hand);
        }, isTorch, blockPos);
    }

    private void doBreak(BlockPos blockPos) {
        if (!canBreak(blockPos)) return;

        if (doBreak.get("Packet")) {
            if (!shouldBreak) return;

            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.DOWN));
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.DOWN));
            shouldBreak = false;
        } else mc.interactionManager.updateBlockBreakingProgress(blockPos, Direction.DOWN);
    }

    private void doAttack(EndCrystalEntity endCrystal) {
        if (endCrystal == null) return;
        HitResult prevResult = mc.crosshairTarget;

        mc.crosshairTarget = new EntityHitResult(endCrystal, PCUtils.closestVec3d(endCrystal));
        PCUtils.leftClick();
        mc.crosshairTarget = prevResult;
        HitMarker.INSTANCE.post();
    }

    private void doRotate(Runnable callback, boolean isTorch, BlockPos blockPos) {
        if (this.direction != null) {
            float yaw = isTorch ? PCUtils.getYaw(torchDirection(blockPos)) : (strictDirection.get() ? mc.player.getYaw() : PCUtils.getYaw(this.direction));
            float pitch = isTorch && torchDirection(blockPos) == Direction.DOWN ? 90 : 0;

            switch (rotations.get()) {
                case "Default" -> RotationUtil.rotate(yaw, pitch, callback);
                case "Strict" -> Rotations.instance.rotate(yaw, pitch, callback);
            }
        } else callback.run();
    }

    private Direction torchDirection(BlockPos blockPos) {
        if (!mc.world.isAir(blockPos.down())) return Direction.DOWN;

        for (Direction direction : Direction.values()) {
            if (direction == Direction.UP) continue;
            BlockState state = mc.world.getBlockState(blockPos.offset(direction));

            if (state.getBlock() instanceof PistonBlock) continue;
            if (state.isAir()) continue;

            return direction;
        }

        return Direction.UP;
    }

    private void doCheck() {
        if (!findInHotbar(Items.END_CRYSTAL).found() || !findInHotbar(Items.PISTON, Items.STICKY_PISTON).found() || !findInHotbar(activator.get("Torch") ? Items.REDSTONE_TORCH : Items.REDSTONE_BLOCK).found() || !findInHotbar(Items.NETHERITE_PICKAXE, Items.DIAMOND_PICKAXE).found()) {
            info(name, "Can't find required items, toggling...");
            toggle();
            return;
        } else Timer.get.timerOverride(timer.get().floatValue());
    }

    private Triplet getPositions(PlayerEntity target) {
        Positions positions = activator.get("Torch") ? new Torch() : new Redstone();
        List<Triplet> init = positions.init(target);

        for (Triplet triplet : init) {
            if (positions.canPlace(triplet)) return triplet;
        }

        return new Triplet(init.get(0).blockPos, Direction.UP);
    }

    private boolean canBreak(BlockPos blockPos) {
        return !mc.world.isAir(blockPos);
    }

    public boolean canPlace(BlockPos blockPos, boolean ignoreEntity) {
        if (blockPos == null) return false;
        if (!mc.world.isAir(blockPos)) return false;
        if (ignoreEntity) return true;

        return !PCUtils.hasEntity(new Box(blockPos), entity -> entity instanceof PlayerEntity || entity instanceof EndCrystalEntity || entity instanceof TntEntity);
    }

    public boolean shouldPause() {
        return isActive() && pauseCA.get();
    }

    public boolean shouldReturn(BlockPos blockPos) {
        return isActive() && currentTriplet != null && currentTriplet.blockPos.contains(blockPos);
    }

    private void debug(String text) {
        if (!debug.get()) return;

        info(name, text);
    }

    @Subscribe
    public void onRender(Render3DEvent event) {
        if (!render.get()) return;
        if (crystalPos == null || pistonPos == null || blockPos == null || (trap.get() && trapPos == null)) return;

        // Crystal
        Box postCrystal = new Box(crystalPos);
        if (crystalBox == null) crystalBox = postCrystal;

        double xC = (postCrystal.minX - crystalBox.minX) / 10;
        double yC = (postCrystal.minY - crystalBox.minY) / 10;
        double zC = (postCrystal.minZ - crystalBox.minZ) / 10;

        crystalBox = new Box(crystalBox.minX + xC, crystalBox.minY + yC, crystalBox.minZ + zC, crystalBox.maxX + xC, crystalBox.maxY + yC, crystalBox.maxZ + zC);
        Vec3d crystalVec3d = Renderer3D.getRenderPosition(crystalBox.getCenter().x - 0.5, crystalBox.getCenter().y - 0.5, crystalBox.getCenter().getZ() - 0.5);
        postCrystal = new Box(crystalVec3d.x, crystalVec3d.y, crystalVec3d.z, crystalVec3d.x + 1.0, crystalVec3d.y + 1.0, crystalVec3d.z + 1.0);

        Renderer3D.get.drawOutlineBox(event.matrix(), postCrystal, HUD.INSTANCE.color());

        // Piston
        Box postPiston = new Box(pistonPos);
        if (pistonBox == null) pistonBox = postPiston;

        double xP = (postPiston.minX - pistonBox.minX) / 10;
        double yP = (postPiston.minY - pistonBox.minY) / 10;
        double zP = (postPiston.minZ - pistonBox.minZ) / 10;

        pistonBox = new Box(pistonBox.minX + xP, pistonBox.minY + yP, pistonBox.minZ + zP, pistonBox.maxX + xP, pistonBox.maxY + yP, pistonBox.maxZ + zP);
        Vec3d pistonVec3d = Renderer3D.getRenderPosition(pistonBox.getCenter().x - 0.5, pistonBox.getCenter().y - 0.5, pistonBox.getCenter().getZ() - 0.5);
        postPiston = new Box(pistonVec3d.x, pistonVec3d.y, pistonVec3d.z, pistonVec3d.x + 1.0, pistonVec3d.y + 1.0, pistonVec3d.z + 1.0);

        Renderer3D.get.drawOutlineBox(event.matrix(), postPiston, HUD.INSTANCE.color());

        // Redstone
        Box postBlockPos = new Box(blockPos);
        if (blockPosBox == null) blockPosBox = postBlockPos;

        double xR = (postBlockPos.minX - blockPosBox.minX) / 10;
        double yR = (postBlockPos.minY - blockPosBox.minY) / 10;
        double zR = (postBlockPos.minZ - blockPosBox.minZ) / 10;

        blockPosBox = new Box(blockPosBox.minX + xR, blockPosBox.minY + yR, blockPosBox.minZ + zR, blockPosBox.maxX + xR, blockPosBox.maxY + yR, blockPosBox.maxZ + zR);
        Vec3d blockPosVec3d = Renderer3D.getRenderPosition(blockPosBox.getCenter().x - 0.5, blockPosBox.getCenter().y - 0.5, blockPosBox.getCenter().getZ() - 0.5);
        postBlockPos = new Box(blockPosVec3d.x, blockPosVec3d.y, blockPosVec3d.z, blockPosVec3d.x + 1.0, blockPosVec3d.y + 1.0, blockPosVec3d.z + 1.0);

        Renderer3D.get.drawOutlineBox(event.matrix(), postBlockPos, HUD.INSTANCE.color());

        // Trap
        Renderer3D.get.drawBoxWithOutline(event.matrix(), trap.get() && canPlace(trapPos, false) ? trapPos : null);
    }

    public enum Stage {
        Preparing, Piston, Crystal, Block, Attack, BreakBlock, BreakPiston
    }

    public static PistonCrystal instance;

    public PistonCrystal() {
        instance = this;
    }
}