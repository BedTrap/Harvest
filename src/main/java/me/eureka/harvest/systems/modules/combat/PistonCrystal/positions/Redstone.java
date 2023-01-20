package me.eureka.harvest.systems.modules.combat.PistonCrystal.positions;

import me.eureka.harvest.systems.modules.combat.PistonCrystal.PistonCrystal;
import me.eureka.harvest.systems.modules.combat.PistonCrystal.Triplet;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Comparator;
import java.util.List;

import static me.eureka.harvest.ic.util.Wrapper.mc;
import static me.eureka.harvest.systems.modules.combat.PistonCrystal.PCUtils.closestVec3d;
import static me.eureka.harvest.systems.modules.combat.PistonCrystal.PCUtils.distanceTo;

public class Redstone extends Positions {

    @Override
    public List<Triplet> init(PlayerEntity target) {
        positions.clear();
        main = target.getBlockPos().up(canUpper(target) ? 2 : 1);

        for (Direction direction : Direction.values()) {
            if (PistonCrystal.instance.strictDirection.get() && mc.player.getHorizontalFacing() != direction) continue;
            if (direction == Direction.UP || direction == Direction.DOWN) continue;
            Direction[] sideDir = sideDirection(direction);

            add(main.offset(direction, 2), main.offset(direction, 3), direction);
            add(main.offset(direction, 2), main.offset(direction, 2).down(), direction);

            add(main.offset(direction, 2).offset(sideDir[0]), main.offset(direction, 3).offset(sideDir[0]), main.offset(direction).offset(sideDir[0]), direction);
            add(main.offset(direction, 2).offset(sideDir[1]), main.offset(direction, 3).offset(sideDir[1]), main.offset(direction).offset(sideDir[1]), direction);

            add(main.offset(direction, 2).offset(sideDir[0]), main.offset(direction, 2).offset(sideDir[0]).down(), main.offset(direction).offset(sideDir[0]), direction);
            add(main.offset(direction, 2).offset(sideDir[1]), main.offset(direction, 2).offset(sideDir[1]).down(), main.offset(direction).offset(sideDir[1]), direction);

            add(main.offset(direction, 2).offset(sideDir[0]), main.offset(direction, 2).offset(sideDir[0], 2), main.offset(direction).offset(sideDir[0]), direction);
            add(main.offset(direction, 2).offset(sideDir[1]), main.offset(direction, 2).offset(sideDir[1], 2), main.offset(direction).offset(sideDir[1]), direction);

            add(main.offset(direction, 2).up(), main.offset(direction, 3).up(), main.offset(direction).up(), direction);
            add(main.offset(direction, 2).up(), main.offset(direction, 2).up(2), main.offset(direction).up(), direction);

            add(main.offset(direction, 2).offset(sideDir[0]).up(), main.offset(direction, 3).offset(sideDir[0]).up(), main.offset(direction).offset(sideDir[0]).up(), direction);
            add(main.offset(direction, 2).offset(sideDir[1]).up(), main.offset(direction, 3).offset(sideDir[1]).up(), main.offset(direction).offset(sideDir[1]).up(), direction);

            add(main.offset(direction, 2).offset(sideDir[0]).up(), main.offset(direction, 2).offset(sideDir[0]).up(2), main.offset(direction).offset(sideDir[0]).up(), direction);
            add(main.offset(direction, 2).offset(sideDir[1]).up(), main.offset(direction, 2).offset(sideDir[1]).up(2), main.offset(direction).offset(sideDir[1]).up(), direction);

            add(main.offset(direction, 2).offset(sideDir[0]).up(), main.offset(direction, 2).offset(sideDir[0], 2).up(), main.offset(direction).offset(sideDir[0]).up(), direction);
            add(main.offset(direction, 2).offset(sideDir[1]).up(), main.offset(direction, 2).offset(sideDir[1], 2).up(), main.offset(direction).offset(sideDir[1]).up(), direction);
        }

        Comparator<Triplet> comparator = Comparator.comparingDouble(Triplet::distance);
        return positions.stream().sorted(PistonCrystal.instance.distance.get("Closest") ? comparator : comparator.reversed()).toList();
    }

    @Override
    public boolean canPlace(Triplet triplet) {
        for (BlockPos blockPos : triplet.blockPos) {
            if (!canCrystal(triplet.blockPos.get(0))) return false;
            if (!canPlace(blockPos, false)) return false;
            if (distanceTo(closestVec3d(blockPos)) > PistonCrystal.instance.placeRange.get()) return false;
        }

        return true;
    }

    private void add(BlockPos piston, BlockPos redstone, Direction direction) {
        positions.add(new Triplet(List.of(main.offset(direction), piston, redstone), direction));
    }

    private void add(BlockPos piston, BlockPos redstone, BlockPos air, Direction direction) {
        positions.add(new Triplet(List.of(main.offset(direction), piston, redstone, air), direction));
    }
}
