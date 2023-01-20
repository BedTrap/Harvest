package me.eureka.harvest.systems.modules.combat.AnchorAura;

import me.eureka.harvest.Hack;
import me.eureka.harvest.events.event.Render3DEvent;
import me.eureka.harvest.events.event.TickEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.ic.util.Renderer3D;
import me.eureka.harvest.mixininterface.IClientPlayerInteractionManager;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.modules.render.LogoutSpot.PlayerCopyEntity;
import me.eureka.harvest.systems.utils.advanced.BlockPosX;
import me.eureka.harvest.systems.managers.DamageManager;
import me.eureka.harvest.systems.utils.advanced.FindItemResult;
import me.eureka.harvest.systems.utils.advanced.InvUtils;
import me.eureka.harvest.systems.utils.other.TimerUtils;
import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static me.eureka.harvest.systems.modules.combat.AnchorAura.AAUtils.*;

@Module.Info(name = "AnchorAura", category = Module.Category.Combat)
public class AnchorAura extends Module {
    public Setting<Boolean> move = register("Move", false);
    public Setting<Double> placeRange = register("PlaceRange", 4.5, 0, 7, 2);
    public Setting<Integer> placeDelay = register("PlaceDelay", 10, 0, 20);
    public Setting<Boolean> airPlace = register("AirPlace", true);
    public Setting<Boolean> support = register("Support", false);

    public Setting<Double> minDMG = register("MinDMG", 8, 0, 36, 2);
    public Setting<Double> maxDMG = register("MaxDMG", 8, 0, 36, 2);

    public Setting<Boolean> safety = register("Safety", false);
    public Setting<String> popOverride = register("PopOverride", List.of("Soft", "Hard", "OFF"), "Hard");

    public Setting<String> render = register("Render", List.of("Box", "Smooth", "OFF"), "Box");
    public Setting<Integer> smoothFactor = register("SmoothFactor", 5, 2, 20);
    public Setting<Boolean> damage = register("Damage", true);

    private final ExecutorService executor = Executors.newCachedThreadPool();

    private BlockPosX bestPos;
    private BlockPosX currentPos;
    private double bestDamage;

    private BlockPosX renderPos;
    private Box smoothBox;

    private Stage stage;

    private final TimerUtils timer = new TimerUtils();

    @Override
    public void onActivate() {
        bestPos = null;
        currentPos = null;
        bestDamage = 0.0;

        renderPos = null;
        smoothBox = null;

        stage = Stage.Place;
    }

    @Subscribe
    private void onTick(TickEvent.Post event) {
        FindItemResult anchor = InvUtils.findInHotbar(Items.RESPAWN_ANCHOR);
        FindItemResult glow = InvUtils.findInHotbar(Items.GLOWSTONE);

        if (!anchor.found() || !glow.found() || mc.world.getDimension().respawnAnchorWorks()) return;
        executor.execute(this::doCalculate);

        doPlace(bestPos, anchor, glow);
    }

    private void doPlace(BlockPosX bp, FindItemResult anchor, FindItemResult glow) {
        if (bp == null) return;
        if (currentPos == null) stage = Stage.Place;

        if (support.get()) {
            switch (stage) {
                case Place -> {
                    if (!timer.passedTicks(placeDelay.get())) return;
                    currentPos = bp;
                    renderPos = bp;

                    placeAnchor(currentPos, anchor);
                    stage = Stage.Charge;
                }
                case Charge -> {
                    if (!timer.passedTicks(placeDelay.get() + 1)) return;
                    placeGlowstone(currentPos, glow);
                }
                case BlowUp -> {
                    if (!timer.passedTicks(placeDelay.get() + 2)) return;
                    breakAnchor(currentPos, anchor);

                    timer.reset();
                    currentPos = null;
                    stage = Stage.Wait;
                }
                case Wait -> {

                }
            }
        } else {
            placeAnchor(currentPos, anchor);
            placeGlowstone(currentPos, glow);
            breakAnchor(currentPos, anchor);

            timer.reset();
        }
    }

    private void doSwap(FindItemResult itemResult) {
        mc.player.getInventory().selectedSlot = itemResult.slot();
        ((IClientPlayerInteractionManager) mc.interactionManager).syncSelected();
    }

    private void placeAnchor(BlockPosX bp, FindItemResult itemResult) {
        BlockPosX dbp = (BlockPosX) getPosition(bp)[0];
        Direction direction = (Direction) getPosition(bp)[1];

        if (support.get()) {
            FindItemResult glowstone = InvUtils.findInHotbar(Items.GLOWSTONE);
            // Matrix AirPlace bypass aka flagging pos
            // Using glowstone because this item will be always with you while you using AnchorAura
            // You are not able to use 2 the same blocks for this trick.
            if (glowstone.found()) {
                doMove(itemResult, () -> {
                    BlockHitResult hitResult = new BlockHitResult(dbp.closestVec3d(), direction, dbp.get(), true);

                    mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hitResult, 0));
                    mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                });
            }
        }

        BlockHitResult hitResult = new BlockHitResult(bp.center(), direction.getOpposite(), bp.get(), true);
        if (move.get()) {
            doMove(itemResult, () -> {
                mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hitResult, 0));
                mc.player.swingHand(Hand.MAIN_HAND);
            });
        } else {
            doSwap(itemResult);
            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hitResult, 0));
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    private void placeGlowstone(BlockPosX bp, FindItemResult itemResult) {
        BlockHitResult hitResult = new BlockHitResult(bp.center(), Direction.UP, bp.get(), false);

        if (move.get()) {
            doMove(itemResult, () -> mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hitResult, 0)));
            mc.player.swingHand(Hand.MAIN_HAND);
        } else {
            doSwap(itemResult);
            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hitResult, 0));
            mc.player.swingHand(Hand.MAIN_HAND);
        }

        stage = Stage.BlowUp;
    }

    private void breakAnchor(BlockPosX bp, FindItemResult itemResult) {
        Direction direction = (mc.player.getY() > bp.y() ? Direction.UP : Direction.DOWN);
        BlockHitResult hitResult = new BlockHitResult(bp.center(), direction, bp.get(), true);

        if (move.get()) {
            doMove(itemResult, () -> mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.OFF_HAND, hitResult, 0)));
            mc.player.swingHand(Hand.OFF_HAND);
        } else {
            doSwap(itemResult);
            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.OFF_HAND, hitResult, 0));
            mc.player.swingHand(Hand.OFF_HAND);
        }
    }

    private void doCalculate() {
        List<BlockPosX> sphere = getSphere(mc.player, Math.ceil(placeRange.get()));

        BlockPosX bestPos = null;
        double bestDamage = 0.0;

        List<PlayerEntity> players = getPlayers();
        sphere = sphere.stream().filter(bp -> someoneIntersects(players, bp, placeRange.get(), airPlace.get(), 0)).toList();

        if (!players.isEmpty()) {
            for (BlockPosX bp : sphere) {
                if (!airPlace.get() && !bp.hasNeighbours()) continue;

                double currentDamage = getHighestDamage(bp);
                if (currentDamage > bestDamage) {
                    bestPos = bp;
                    bestDamage = currentDamage;
                }
            }
        }

        this.bestPos = bestPos;
        this.bestDamage = bestDamage;
    }

    private double getHighestDamage(BlockPosX bp) {
        if (bp == null) return 0.0;

        double highestDamage = 0.0;
        double selfImpact = 0.0;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player.isCreative() || player.isSpectator()) continue;
            if (player instanceof PlayerCopyEntity) continue;
            if (Hack.friends().isFriend(player)) continue;
            if (player == mc.player) continue;
            if (player.isDead()) continue;

            double playerDamage = DamageManager.anchorDamage(player, bp, true);
            double selfDamage = DamageManager.anchorDamage(mc.player, bp, true);

            if (popOverride.get("Hard") && playerDamage >= totalHealth(player)) return 999.0;
            if (safety.get() && selfDamage > totalHealth(mc.player)) continue;
            if (popOverride.get("Soft") && playerDamage >= totalHealth(player)) return 999.0;

            if (playerDamage < minDMG.get()) continue;
            if (selfDamage > maxDMG.get()) continue;

            highestDamage = Math.max(playerDamage, highestDamage);
            selfImpact = Math.max(selfDamage, selfImpact);
        }

        return highestDamage - selfImpact;
    }

    private Object[] getPosition(BlockPosX bp) {
        double distance = 999.9;
        BlockPosX position = null;
        Direction direction = null;

        for (Direction dir : Direction.values()) {
            BlockPosX cbp = bp.offset(dir);
            if (cbp.hasEntity()) continue;

            double dis = cbp.distance();
            if (dis < distance) {
                distance = dis;
                position = cbp;
                direction = dir;
            }
        }

        return new Object[]{position, direction};
    }

    private double getBestDamage() {
        return ((double) Math.round(bestDamage * 100) / 100);
    }

    @Subscribe
    public void onRender(Render3DEvent event) {
        if (renderPos == null) return;

        switch (render.get()) {
            case "Box" -> {
                Renderer3D.get.drawBoxWithOutline(event.matrix(), renderPos.get());

                if (!damage.get()) return;
                Vec3d dv = renderPos.center(); // Damage
                Renderer3D.drawText(Text.of(String.valueOf(getBestDamage())), dv.x, dv.y, dv.z, 0.8, false);
            }
            case "Smooth" -> {
                Box post = new Box(renderPos);
                if (smoothBox == null) smoothBox = post;

                double x = (post.minX - smoothBox.minX) / smoothFactor.get();
                double y = (post.minY - smoothBox.minY) / smoothFactor.get();
                double z = (post.minZ - smoothBox.minZ) / smoothFactor.get();

                smoothBox = new Box(smoothBox.minX + x, smoothBox.minY + y, smoothBox.minZ + z, smoothBox.maxX + x, smoothBox.maxY + y, smoothBox.maxZ + z);

                Vec3d dv = smoothBox.getCenter(); // Damage
                Vec3d bv = Renderer3D.getRenderPosition(smoothBox.getCenter()); // Box

                Box box = new Box(bv.x - 0.5, bv.y - 0.5, bv.z - 0.5, bv.x + 0.5, bv.y + 0.5, bv.z + 0.5);
                Renderer3D.get.drawBoxWithOutline(event.matrix(), box);

                if (!damage.get()) return;
                Renderer3D.drawText(Text.of(String.valueOf(getBestDamage())), dv.x, dv.y, dv.z, 0.8, false);
            }
        }
    }

    private double totalHealth(PlayerEntity player) {
        return player.getHealth() + player.getAbsorptionAmount();
    }

    public enum Stage {
        Place, Charge, BlowUp, Wait
    }
}
