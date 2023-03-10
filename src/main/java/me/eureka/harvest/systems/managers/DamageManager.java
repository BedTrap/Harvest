package me.eureka.harvest.systems.managers;

import me.eureka.harvest.Hack;
import me.eureka.harvest.events.event.GameJoinedEvent;
import me.eureka.harvest.mixininterface.IExplosion;
import me.eureka.harvest.mixininterface.IRaycastContext;
import me.eureka.harvest.mixininterface.IVec3d;
import com.google.common.eventbus.Subscribe;
import me.eureka.harvest.ic.util.Wrapper;
import me.eureka.harvest.systems.utils.advanced.BlockPosX;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.explosion.Explosion;

import static me.eureka.harvest.ic.util.Wrapper.mc;

public class DamageManager {
    private static final Vec3d vec3d = new Vec3d(0, 0, 0);
    public static Explosion explosion;
    public static RaycastContext raycastContext;

    public DamageManager() {
        Hack.event().register(this);
    }

    @Subscribe
    private void onJoin(GameJoinedEvent event) {
        explosion = new Explosion(mc.world, null, 0, 0, 0, 6, false, Explosion.DestructionType.DESTROY);
        raycastContext = new RaycastContext(null, null, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, mc.player);
    }

    public static double crystalDamage(PlayerEntity player, Vec3d crystal, BlockPos obsidianPos, boolean ignoreTerrain) {
        if (player == null) return 0;
        if (player.isCreative()) return 0;

        ((IVec3d) vec3d).set(player.getPos().x, player.getPos().y, player.getPos().z);

        double modDistance = Math.sqrt(vec3d.squaredDistanceTo(crystal));
        if (modDistance > 12) return 0;

        double exposure = getExposure(crystal, player, raycastContext, obsidianPos, ignoreTerrain);
        double impact = (1 - (modDistance / 12)) * exposure;
        double damage = ((impact * impact + impact) / 2 * 7 * (6 * 2) + 1);

        damage = getDamageForDifficulty(damage);
        damage = DamageUtil.getDamageLeft((float) damage, (float) player.getArmor(), (float) player.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS).getValue());
        damage = resistanceReduction(player, damage);

        ((IExplosion) explosion).set(crystal, 6, false);
        damage = blastProtReduction(player, damage, explosion);

        return damage < 0 ? 0 : damage;
    }

    public static double crystalDamage(PlayerEntity player, Vec3d crystal) {
        return crystalDamage(player, crystal, null, false);
    }

    public static double getSwordDamage(PlayerEntity player) {
        double damage = 0;
        if (mc.player.getMainHandStack().getItem() == Items.NETHERITE_SWORD) {
            damage += 8;
        } else if (mc.player.getMainHandStack().getItem() == Items.DIAMOND_SWORD) {
            damage += 7;
        } else if (mc.player.getMainHandStack().getItem() == Items.GOLDEN_SWORD) {
            damage += 4;
        } else if (mc.player.getMainHandStack().getItem() == Items.IRON_SWORD) {
            damage += 6;
        } else if (mc.player.getMainHandStack().getItem() == Items.STONE_SWORD) {
            damage += 5;
        } else if (mc.player.getMainHandStack().getItem() == Items.WOODEN_SWORD) {
            damage += 4;
        }
        damage *= 1.5;

        if (mc.player.getMainHandStack().getEnchantments() != null) {
            if (EnchantmentHelper.get(mc.player.getMainHandStack()).containsKey(Enchantments.SHARPNESS)) {
                int level = EnchantmentHelper.getLevel(Enchantments.SHARPNESS, mc.player.getMainHandStack());
                damage += (0.5 * level) + 0.5;
            }
        }

        // Reduce by resistance
        damage = resistanceReduction(player, damage);

        // Reduce by armour
        damage = DamageUtil.getDamageLeft((float) damage, (float) player.getArmor(), (float) player.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS).getValue());

        // Reduce by enchants
        damage = normalProtReduction(player, damage);

        return damage;
    }

    // Utils

    private static double getDamageForDifficulty(double damage) {
        return switch (mc.world.getDifficulty()) {
            case PEACEFUL -> 0;
            case EASY -> Math.min(damage / 2 + 1, damage);
            case HARD -> damage * 3 / 2;
            default -> damage;
        };
    }

    private static double normalProtReduction(Entity player, double damage) {
        int protLevel = EnchantmentHelper.getProtectionAmount(player.getArmorItems(), DamageSource.GENERIC);
        if (protLevel > 20) protLevel = 20;

        damage *= 1 - (protLevel / 25.0);
        return damage < 0 ? 0 : damage;
    }

    private static double blastProtReduction(Entity player, double damage, Explosion explosion) {
        int protLevel = EnchantmentHelper.getProtectionAmount(player.getArmorItems(), DamageSource.explosion(explosion));
        if (protLevel > 20) protLevel = 20;

        damage *= (1 - (protLevel / 25.0));
        return damage < 0 ? 0 : damage;
    }

    private static double resistanceReduction(LivingEntity player, double damage) {
        if (player.hasStatusEffect(StatusEffects.RESISTANCE)) {
            int lvl = (player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() + 1);
            damage *= (1 - (lvl * 0.2));
        }

        return damage < 0 ? 0 : damage;
    }

    private static double getExposure(Vec3d source, Entity entity, RaycastContext raycastContext, BlockPos obsidianPos, boolean ignoreTerrain) {
        Box box = entity.getBoundingBox();

        double d = 1 / ((box.maxX - box.minX) * 2 + 1);
        double e = 1 / ((box.maxY - box.minY) * 2 + 1);
        double f = 1 / ((box.maxZ - box.minZ) * 2 + 1);
        double g = (1 - Math.floor(1 / d) * d) / 2;
        double h = (1 - Math.floor(1 / f) * f) / 2;

        if (!(d < 0) && !(e < 0) && !(f < 0)) {
            int i = 0;
            int j = 0;

            for (double k = 0; k <= 1; k += d) {
                for (double l = 0; l <= 1; l += e) {
                    for (double m = 0; m <= 1; m += f) {
                        double n = MathHelper.lerp(k, box.minX, box.maxX);
                        double o = MathHelper.lerp(l, box.minY, box.maxY);
                        double p = MathHelper.lerp(m, box.minZ, box.maxZ);

                        ((IVec3d) vec3d).set(n + g, o, p + h);
                        ((IRaycastContext) raycastContext).set(vec3d, source, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, entity);

                        if (raycast(raycastContext, obsidianPos, ignoreTerrain).getType() == HitResult.Type.MISS) i++;

                        j++;
                    }
                }
            }

            return (double) i / j;
        }

        return 0;
    }

    private static BlockHitResult raycast(RaycastContext context, BlockPos obsidianPos, boolean ignoreTerrain) {
        return BlockView.raycast(context.getStart(), context.getEnd(), context, (raycastContext, blockPos) -> {
            BlockState blockState;
            if (blockPos.equals(obsidianPos)) blockState = Blocks.OBSIDIAN.getDefaultState();
            else {
                blockState = mc.world.getBlockState(blockPos);
                if (blockState.getBlock().getBlastResistance() < 600 && ignoreTerrain)
                    blockState = Blocks.AIR.getDefaultState();
            }

            Vec3d vec3d = raycastContext.getStart();
            Vec3d vec3d2 = raycastContext.getEnd();

            VoxelShape voxelShape = raycastContext.getBlockShape(blockState, mc.world, blockPos);
            BlockHitResult blockHitResult = mc.world.raycastBlock(vec3d, vec3d2, blockPos, voxelShape, blockState);
            VoxelShape voxelShape2 = VoxelShapes.empty();
            BlockHitResult blockHitResult2 = voxelShape2.raycast(vec3d, vec3d2, blockPos);

            double d = blockHitResult == null ? Double.MAX_VALUE : raycastContext.getStart().squaredDistanceTo(blockHitResult.getPos());
            double e = blockHitResult2 == null ? Double.MAX_VALUE : raycastContext.getStart().squaredDistanceTo(blockHitResult2.getPos());

            return d <= e ? blockHitResult : blockHitResult2;
        }, (raycastContext) -> {
            Vec3d vec3d = raycastContext.getStart().subtract(raycastContext.getEnd());
            return BlockHitResult.createMissed(raycastContext.getEnd(), Direction.getFacing(vec3d.x, vec3d.y, vec3d.z), new BlockPos(raycastContext.getEnd()));
        });
    }

    public static double anchorDamage(PlayerEntity player, BlockPosX bp, boolean ignoreTerrain) {
        return bedDamage(player, bp.get(), ignoreTerrain);
    }

    public static double bedDamage(PlayerEntity player, BlockPos pos, boolean ignoreTerrain) {
        if (player == null || player.getAbilities().creativeMode) return 0;

        Vec3d position = Vec3d.ofCenter(pos);
        if (explosion == null) explosion = new Explosion(mc.world, null, position.x, position.y, position.z, 5.0F, true, Explosion.DestructionType.DESTROY);
        else ((IExplosion) explosion).set(position, 5.0F, true);

        double distance = Math.sqrt(player.squaredDistanceTo(position));
        if (distance > 10.5) return 0;

        if (raycastContext == null) raycastContext = new RaycastContext(null, null, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, mc.player);

        double exposure = getBedExposure(position, player, raycastContext, pos, ignoreTerrain);
        double impact = (1.0 - (distance / 10.0)) * exposure;
        double damage = (impact * impact + impact) / 2 * 7 * (5 * 2) + 1;

        EntityAttributeInstance attribute = player.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS);

        // Damage calculation
        damage = getDamageForDifficulty(damage);
        damage = resistanceReduction(player, damage);
        damage = DamageUtil.getDamageLeft((float) damage, (float) player.getArmor(), attribute != null ? (float) attribute.getValue() : 0);
        damage = blastProtReduction(player, damage, explosion);

        return damage < 0 ? 0 : damage;
    }

    private static double getBedExposure(Vec3d source, Entity entity, RaycastContext context, BlockPos pos, boolean ignoreTerrain) {
        Box box = entity.getBoundingBox();

        double xFactor = 1.0 / ((box.maxX - box.minX) * 2.0 + 1.0);
        double yFactor = 1.0 / ((box.maxY - box.minY) * 2.0 + 1.0);
        double zFactor = 1.0 / ((box.maxZ - box.minZ) * 2.0 + 1.0);

        double dX = (1.0 - Math.floor(1.0 / xFactor) * xFactor) / 2.0;
        double dZ = (1.0 - Math.floor(1.0 / zFactor) * zFactor) / 2.0;

        if (xFactor >= 0.0 && yFactor >= 0.0 && zFactor >= 0.0) {
            int miss = 0;
            int hit = 0;

            for (double x = 0.0; x <= 1.0; x += xFactor) {
                for (double y = 0.0; y <= 1.0; y += yFactor) {
                    for (double z = 0.0; z <= 1.0; z += zFactor) {
                        double nX = MathHelper.lerp(x, box.minX, box.maxX);
                        double nY = MathHelper.lerp(y, box.minY, box.maxY);
                        double nZ = MathHelper.lerp(z, box.minZ, box.maxZ);

                        ((IRaycastContext) context).set(new Vec3d(nX + dX, nY, nZ + dZ), source, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, entity);
                        if (bedRaycast(context, pos, ignoreTerrain).getType() == HitResult.Type.MISS) miss++;

                        hit++;
                    }
                }
            }

            return (float) miss / (float) hit;
        }

        return 0.0F;
    }

    private static BlockHitResult bedRaycast(RaycastContext context, BlockPos bed, boolean ignoreTerrain) {
        return BlockView.raycast(context.getStart(), context.getEnd(), context, (raycast, pos) -> {
            BlockState state = mc.world.getBlockState(pos);
            if (ignoreTerrain && state.getBlock().getBlastResistance() < 600.0F) state = Blocks.AIR.getDefaultState();
            if (bed != null && state.getBlock() instanceof BedBlock) state = Blocks.AIR.getDefaultState();

            Vec3d start = raycast.getStart();
            Vec3d end = raycast.getEnd();

            BlockHitResult blockResult = mc.world.raycastBlock(start, end, pos, raycast.getBlockShape(state, mc.world, pos), state);
            BlockHitResult emptyResult = VoxelShapes.empty().raycast(start, end, pos);

            double block = blockResult == null ? Double.MAX_VALUE : raycast.getStart().squaredDistanceTo(blockResult.getPos());
            double empty = emptyResult == null ? Double.MAX_VALUE : raycast.getStart().squaredDistanceTo(emptyResult.getPos());

            return block <= empty ? blockResult : emptyResult;
        }, (raycast) -> {
            Vec3d diff = raycast.getStart().subtract(raycast.getEnd());
            return BlockHitResult.createMissed(raycast.getEnd(), Direction.getFacing(diff.x, diff.y, diff.z), new BlockPos(raycast.getEnd()));
        });
    }
}