package me.eureka.harvest.systems.modules.movement.Speed;

import me.eureka.harvest.events.event.MoveEvent;
import me.eureka.harvest.events.event.PacketEvent;
import me.eureka.harvest.events.event.TickEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.mixininterface.IVec3d;
import me.eureka.harvest.mixins.PlayerPositionLookS2CPacketAccessor;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.modules.movement.HoleSnap.HoleSnap;
import me.eureka.harvest.systems.modules.world.Timer.Timer;
import me.eureka.harvest.systems.utils.advanced.BlockPosX;
import me.eureka.harvest.systems.utils.other.TimerUtils;
import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Module.Info(name = "Speed", category = Module.Category.Movement)
public class Speed extends Module {
    public Setting<Double> baseSpeedValue = register("BaseSpeed", 0.29, 0, 0.75, 2);

    public Setting<String> antiCheat = register("AntiCheat", List.of("NCP", "Vanilla"), "NCP");
    public Setting<String> mode = register("Speed", List.of("Strafe", "StrafeStrict", "StrafeLow", "StrafeGround", "OnGround"), "Strafe");
    public Setting<String> friction = register("Friction", List.of("Factor", "Fast", "Strict"), "Factor");
    public Setting<Double> timer = register("Timer", 1.0, 0.1, 2.0, 2);
    public Setting<Boolean> resetTimer = register("ResetTimer", true);
    public Setting<Integer> setBackDelay = register("SetBackDelay", 20, 0, 60);
    public Setting<Boolean> retain = register("Retain", false);
    public Setting<Boolean> airStrafe = register("AirStrafe", true);
    public Setting<String> autoJump = register("AutoJump", List.of("Default", "LowHop", "LowStrict", "OFF"), "OFF");

    public Setting<Boolean> boost = register("Boost", false);
    public Setting<Double> multiply = register("Multiply", 0.15, 0.0, 0.25, 2);
    public Setting<Double> maximum = register("Maximum", 0.16, 0.0, 0.26, 2);

    public Setting<Boolean> strictSprint = register("StrictSprint", false);
    public Setting<Boolean> strictJump = register("StrictJump", false);
    public Setting<Boolean> strictCollision = register("StrictCollision", false);

    public Setting<Boolean> inLiquids = register("InLiquids", true);
    public Setting<Boolean> whenSneaking = register("WhenSneaking", false);

    private double playerSpeed;
    private double latestMoveSpeed;
    private double boostSpeed;

    private int timersTick;
    private boolean accelerate;
    private boolean offsetPackets;

    private int strictTicks;

    private StrafeStage strafeStage = StrafeStage.Speed;
    private GroundStage groundStage = GroundStage.CheckSpace;

    private final TimerUtils setbackTimer = new TimerUtils();
    private final TimerUtils boostTimer = new TimerUtils();

    private int teleportId;

    @Override
    public void onDeactivate() {
        Timer.get.timerOverride(1.0f);
    }

    @Subscribe
    private void onTick(TickEvent.Post event){
        latestMoveSpeed = Math.sqrt(StrictMath.pow(mc.player.getX() - mc.player.prevX, 2) + StrictMath.pow(mc.player.getZ() - mc.player.prevZ, 2));
    }

    @Subscribe
    private void onPlayerMove(MoveEvent.Player event){
        event.setCancelled(true);
        if (HoleSnap.INSTANCE.snapped()) return;

        if (!setbackTimer.passedTicks(setBackDelay.get())) return;
        if (!whenSneaking.get() && mc.player.isSneaking()) return;
        if (!inLiquids.get() && (mc.player.isTouchingWater() || mc.player.isInLava())) return;

        double baseSpeed = getBaseMoveSpeed(baseSpeedValue.get());

        if (strictSprint.get() && !mc.player.isSprinting()){
            if (mc.getNetworkHandler() != null){
                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
            }
        }

        switch (mode.get()){
            case "Strafe", "StrafeStrict", "StrafeLow", "StrafeGround" -> {
                // [Timer] //
                if (resetTimer.get()){
                    timersTick++;
                    if (timersTick >= 5){
                        Timer.get.timerOverride(1.0f);
                        timersTick = 0;
                    } else if (isMoving()){
                        Timer.get.timerOverride(timer.get().floatValue());

                        // [slight boost] //
                        ((IVec3d) event.movement).setXZ(event.movement.x * 1.02, event.movement.z * 1.02);
                    }
                }else {
                    Timer.get.timerOverride(isMoving() ? timer.get().floatValue() : 1.0f);
                }


                if (isMoving()){
                    if (mc.player.isOnGround()) strafeStage = StrafeStage.Start;

                    // [Check burrow] //
                    if (mode.get("Strafe") || mode.get("StrafeLow")){
                        //if (BlockHelper.isSolid(mc.player.getBlockPos())) return;
                    }
                }

                // [On Fall] //
                if (mode.get("StrafeStrict")){
                    double yDifference = mc.player.getY() - Math.floor(mc.player.getY());

                    if (roundDouble(yDifference, 3) == roundDouble(0.138, 3)){
                        strafeStage = StrafeStage.Fall;

                        // [Falling motion] //
                        ((IVec3d) mc.player.getVelocity()).setY(mc.player.getVelocity().y -0.08);
                        ((IVec3d) event.movement).setY(event.movement.y - 0.09316090325960147);
                        ((IVec3d) mc.player.getPos()).setY(mc.player.getPos().y - 0.09316090325960147);
                    }
                }

                if (strafeStage != StrafeStage.Collision || !isMoving()){
                    // [start jumping] //

                    if (strafeStage == StrafeStage.Start){
                        strafeStage = StrafeStage.Jump;
                        double jumpSpeed = 0.3999999463558197;

                        if (strictJump.get()) jumpSpeed = 0.42;

                        if (autoJump.get("Default")){
                            if (mode.get("StrafeLow")) jumpSpeed = 0.31;
                            else jumpSpeed = 0.42;
                        }
                        else if (mode.get("StrafeLow")) jumpSpeed = 0.27;

                        if (autoJump.get("LowHop")) jumpSpeed = 0.38;

                        if (mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST))
                            jumpSpeed += (mc.player.getStatusEffect(StatusEffects.JUMP_BOOST).getAmplifier() + 1) * 0.1;

                        // [jump] //
                        if (!autoJump.get("OFF")){
                            if (autoJump.get("LowStrict")) {
                                jump(0.38);
                            } else {
                                ((IVec3d) mc.player.getVelocity()).setY(jumpSpeed);
                                ((IVec3d) event.movement).setY(jumpSpeed);
                            }

                            double acceleration = 2.149;

                            if (mode.get("Strafe")){
                                acceleration = 1.395;
                                if (accelerate) {
                                    acceleration = 1.6835;
                                }
                            }

                            playerSpeed *= acceleration;
                        }
                    }
                    else if (strafeStage == StrafeStage.Jump){
                        strafeStage = StrafeStage.Speed;
                        double scaledMoveSpeed = 3 * (latestMoveSpeed - baseSpeed);
                        playerSpeed = latestMoveSpeed - scaledMoveSpeed;
                        accelerate = !accelerate;
                    }
                    else {
                        if (!mc.world.isSpaceEmpty(mc.player.getBoundingBox().offset(0.0, mc.player.getVelocity().y, 0.0)) || mc.player.verticalCollision){
                            strafeStage = StrafeStage.Collision;

                            if (retain.get()) strafeStage = StrafeStage.Start;
                        }

                        double collisionSpeed = latestMoveSpeed - (latestMoveSpeed / 159);
                        if (strictCollision.get()){
                            collisionSpeed = baseSpeed;
                            latestMoveSpeed = 0;
                        }
                        playerSpeed = collisionSpeed;
                    }
                }
                else {
                    if (mc.player.isOnGround()) strafeStage = StrafeStage.Start;
                    if (new BlockPosX(mc.player.getBlockPos()).replaceable()){
                        playerSpeed = baseSpeed * 1.38;
                    }
                }

                playerSpeed = Math.max(playerSpeed, baseSpeed);

                if (mode.get("StrafeStrict")){
                    playerSpeed = Math.min(baseSpeed, strictTicks > 25 ? 0.465 : 0.44);
                }

                strictTicks++;

                if (strictTicks > 50) {
                    strictTicks = 0;
                }

                if (friction.get("Factor")){
                    float friction = 1;
                    if (mc.player.isTouchingWater()) friction = 0.89F;
                    else if (mc.player.isInLava()) friction = 0.535F;
                    playerSpeed *= friction;
                }

                float forward = mc.player.input.movementForward;
                float strafe = mc.player.input.movementSideways;
                float yaw = mc.player.getYaw();

                if (mode.get("StrafeStrict")){
                    if (!isMoving()){
                        ((IVec3d) event.movement).setXZ(0, 0);
                    }
                    else if (forward != 0){
                        if (strafe >= 1) {
                            yaw += (forward > 0 ? -45 : 45);
                            strafe = 0;
                        }

                        else if (strafe <= -1) {
                            yaw += (forward > 0 ? 45 : -45);
                            strafe = 0;
                        }

                        if (forward > 0) {
                            forward = 1;
                        }

                        else if (forward < 0) {
                            forward = -1;
                        }
                    }
                }
                else {
                    if (!isMoving()){
                        ((IVec3d) event.movement).setXZ(0, 0);
                    }
                    else if (forward != 0){
                        if (strafe > 0) yaw += forward > 0 ? -45 : 45;
                        else if (strafe < 0) yaw += forward > 0 ? 45 : -45;
                        strafe = 0;
                        if (forward > 0) forward = 1;
                        else if (forward < 0) forward = -1;
                    }
                }

                if (boost.get() && !boostTimer.passedMillis(50))
                    playerSpeed += Math.min(boostSpeed * multiply.get(), maximum.get());

                double cos = Math.cos(Math.toRadians(yaw + 90));
                double sin = Math.sin(Math.toRadians(yaw + 90));

                double x = (forward * playerSpeed * cos) + (strafe * playerSpeed * sin);
                double z = (forward * playerSpeed * sin) - (strafe * playerSpeed * cos);

                ((IVec3d) event.movement).setXZ(x, z);

                if (!isMoving()){
                    ((IVec3d) event.movement).setXZ(0, 0);
                }
            }
            case "OnGround" -> {
                if (mc.player.isOnGround() && isMoving()){
                    if (groundStage == GroundStage.FakeJump){
                        offsetPackets = true;
                        double acceleration = 2.149;
                        playerSpeed -= acceleration;
                        groundStage = GroundStage.Speed;
                    }
                    else if (groundStage == GroundStage.Speed){
                        double scaledMoveSpeed = 0.66 * (latestMoveSpeed - baseSpeed);
                        playerSpeed = latestMoveSpeed - scaledMoveSpeed;
                        groundStage = GroundStage.FakeJump;
                    }

                    if (!mc.world.isSpaceEmpty(mc.player.getBoundingBox().offset(0.0, mc.player.getVelocity().y, 0.0)) || mc.player.verticalCollision){
                        groundStage = GroundStage.FakeJump;
                        double collisionSpeed = latestMoveSpeed - (latestMoveSpeed / 159);
                        if (strictCollision.get()){
                            collisionSpeed = baseSpeed;
                            latestMoveSpeed = 0;
                        }

                        playerSpeed = collisionSpeed;
                    }
                }

                if (airStrafe.get() && !mc.player.isOnGround()){
                    ((IVec3d) mc.player.getVelocity()).setY(mc.player.getVelocity().y / 1.1);
                }

                if (boost.get() && !boostTimer.passedMillis(50))
                    playerSpeed += Math.min(boostSpeed * multiply.get(), maximum.get());

                double[] dir = transformStrafe((float) playerSpeed);
                ((IVec3d) event.movement).setXZ(dir[0], dir[1]);

                ((IVec3d) event.movement).setXZ(dir[0], dir[1]);
            }
        }

    }


    @Subscribe
    private void onPacket(PacketEvent.Receive event){
        if (event.packet instanceof PlayerPositionLookS2CPacket packet){
/*            if (mc.player.isAlive()){
                if (this.teleportId <= 0) {
                    this.teleportId = ((PlayerPositionLookS2CPacket) event.packet).getTeleportId();
                } else {
                    if (mc.world.isPosLoaded(mc.player.getBlockX(), mc.player.getBlockZ())){
                        setbackTimer.reset();
                        ((PlayerPositionLookS2CPacketAccessor) packet).setX(packet.getX());
                        ((PlayerPositionLookS2CPacketAccessor) packet).setY(packet.getY());
                        ((PlayerPositionLookS2CPacketAccessor) packet).setZ(packet.getZ());
                        reset();
                    }
                }

            }*/
            ((PlayerPositionLookS2CPacketAccessor) event.packet).setYaw(mc.player.getYaw());
            ((PlayerPositionLookS2CPacketAccessor) event.packet).setPitch(mc.player.getPitch());
            packet.getFlags().remove(PlayerPositionLookS2CPacket.Flag.X_ROT);
            packet.getFlags().remove(PlayerPositionLookS2CPacket.Flag.Y_ROT);
            teleportId = packet.getTeleportId();
        }
        if (event.packet instanceof ExplosionS2CPacket explosionS2CPacket){
            boostSpeed = Math.abs((explosionS2CPacket.getPlayerVelocityX()) + Math.abs(explosionS2CPacket.getPlayerVelocityZ()));
            boostTimer.reset();
        }
        if (event.packet instanceof EntityVelocityUpdateS2CPacket entityVelocityUpdateS2CPacket){
            if (entityVelocityUpdateS2CPacket.getId() != mc.player.getId()) return;

            if (boostSpeed < Math.abs(entityVelocityUpdateS2CPacket.getVelocityX()) + Math.abs(entityVelocityUpdateS2CPacket.getVelocityZ())) {
                boostSpeed = Math.abs(entityVelocityUpdateS2CPacket.getVelocityX()) + Math.abs(entityVelocityUpdateS2CPacket.getVelocityZ());
                boostTimer.reset();
            }
        }
    }

    // [Misc Util] //

    public double roundDouble(double number, int scale) {
        BigDecimal bigDecimal = new BigDecimal(number);
        bigDecimal = bigDecimal.setScale(scale, RoundingMode.HALF_UP);
        return bigDecimal.doubleValue();
    }

    private void reset(){
        strafeStage = StrafeStage.Collision;
        playerSpeed = 0;
        latestMoveSpeed = 0;
        strictTicks = 0;
        accelerate = false;
        teleportId = 0;
    }

    private double[] transformStrafe(double speed) {
        float forward = mc.player.input.movementForward;
        float side = mc.player.input.movementSideways;
        float yaw = mc.player.prevYaw + (mc.player.getYaw() - mc.player.prevYaw) * mc.getTickDelta();

        double velX, velZ;

        if (forward == 0.0f && side == 0.0f) return new double[]{0, 0};

        else if (forward != 0.0f) {
            if (side >= 1.0f) {
                yaw += (float) (forward > 0.0f ? -45 : 45);
                side = 0.0f;
            } else if (side <= -1.0f) {
                yaw += (float) (forward > 0.0f ? 45 : -45);
                side = 0.0f;
            }

            if (forward > 0.0f)
                forward = 1.0f;

            else if (forward < 0.0f)
                forward = -1.0f;
        }

        double mx = Math.cos(Math.toRadians(yaw + 90.0f));
        double mz = Math.sin(Math.toRadians(yaw + 90.0f));

        velX = (double) forward * speed * mx + (double) side * speed * mz;
        velZ = (double) forward * speed * mz - (double) side * speed * mx;

        return new double[]{velX, velZ};
    }

    private double getBaseMoveSpeed(double defaultSpeed) {
        if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
            int amplifier = mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier();
            defaultSpeed *= 1.0 + 0.2 * (amplifier + 1);
        }
        if (mc.player.hasStatusEffect(StatusEffects.SLOWNESS)) {
            int amplifier = mc.player.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier();
            defaultSpeed /= 1.0 + 0.2 * (amplifier + 1);
        }
        return defaultSpeed;
    }

    private boolean isMoving() {
        return mc.player.input.movementSideways != 0 || mc.player.input.movementForward != 0;
    }

    private void jump(double height) {
        if (height > 0.42) height = 0.42;
        Vec3d vec3d = mc.player.getVelocity();
        mc.player.setVelocity(vec3d.x, height, vec3d.z);
        if (mc.player.isSprinting()) {
            float f = mc.player.getYaw() * 0.017453292F;
            mc.player.setVelocity(mc.player.getVelocity().add(-MathHelper.sin(f) * 0.2F, 0.0, MathHelper.cos(f) * 0.2F));
        }
    }

    public enum StrafeStage {
        Collision, Start, Jump, Fall, Speed
    }

    public enum GroundStage {
        Speed, FakeJump, CheckSpace
    }
}