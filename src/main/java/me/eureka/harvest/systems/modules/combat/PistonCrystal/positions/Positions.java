package me.eureka.harvest.systems.modules.combat.PistonCrystal.positions;

import me.eureka.harvest.systems.modules.combat.PistonCrystal.PistonCrystal;
import me.eureka.harvest.systems.modules.combat.PistonCrystal.Triplet;
import net.minecraft.block.Blocks;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

import static me.eureka.harvest.ic.util.Wrapper.mc;
import static me.eureka.harvest.systems.modules.combat.PistonCrystal.PCUtils.hasEntity;

public abstract class Positions {
    public final List<Triplet> positions = new ArrayList<>();
    public static BlockPos main;

    public abstract List<Triplet> init(PlayerEntity target);
    public abstract boolean canPlace(Triplet triplet);

    public boolean canPlace(BlockPos blockPos, boolean ignoreEntity) {
        if (blockPos == null) return false;
        if (!mc.world.isAir(blockPos)) return false;
        if (ignoreEntity) return true;

        return !hasEntity(new Box(blockPos), entity -> entity instanceof PlayerEntity || entity instanceof EndCrystalEntity || entity instanceof TntEntity);
    }

    public boolean canCrystal(BlockPos blockPos) {
        if (hasEntity(new Box(blockPos))) return false;

        return mc.world.isAir(blockPos) &&
                (mc.world.getBlockState(blockPos.down()).isOf(Blocks.OBSIDIAN) || mc.world.getBlockState(blockPos.down()).isOf(Blocks.BEDROCK));
    }

    public Direction[] sideDirection(Direction direction) {
        if (direction == Direction.WEST || direction == Direction.EAST) {
            return new Direction[]{Direction.NORTH, Direction.SOUTH};
        } else {
            return new Direction[]{Direction.WEST, Direction.EAST};
        }
    }

    public static boolean isUpper() {
        return main.equals(PistonCrystal.instance.target.getBlockPos().up(2)) && !PistonCrystal.instance.canPlace(PistonCrystal.instance.trapPos, true);
    }

    public boolean canUpper(PlayerEntity target) {
        return PistonCrystal.instance.allowUpper.get() && (!facePlace(target) || (PistonCrystal.instance.stacked != null && PistonCrystal.instance.stacked.blockPos.get(0).getY() == target.getBlockPos().getY() + 1 && PistonCrystal.instance.stacked.direction == Direction.UP));
    }

    public boolean facePlace(PlayerEntity target) {
        if (target == null) return false;
        BlockPos targetPos = target.getBlockPos().up();

        for (Direction direction : Direction.values()) {
            if (canCrystal(targetPos.offset(direction))) return true;
        }

        return false;
    }
}
