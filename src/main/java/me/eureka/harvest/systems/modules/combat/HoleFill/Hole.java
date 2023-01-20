package me.eureka.harvest.systems.modules.combat.HoleFill;

import me.eureka.harvest.systems.utils.advanced.BlockPosX;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

import static me.eureka.harvest.ic.util.Wrapper.mc;

public class Hole {
    public static boolean singleHole(BlockPosX bp) {
        for (int i = 0; i < 3; i++) {
            if (!bp.up(i).air()) return false;
        }

        for (Direction direction : Direction.values()) {
            if (direction == Direction.UP || direction == Direction.DOWN) continue;

            if (!bp.offset(direction).blastRes()) return false;
        }

        return true;
    }

    public static boolean doubleHole(BlockPosX bp, boolean ignoreWithPlayer) {
        for (int i = 0; i < 3; i++) {
            if (!bp.up(i).air()) return false;
        }
        BlockPosX secondPos = null;

        int i = 0;
        for (Direction direction : Direction.values()) {
            if (direction == Direction.UP || direction == Direction.DOWN) continue;

            if (bp.offset(direction).blastRes()) {
                i++;
            } else {
                if (bp.offset(direction).air()) secondPos = bp.offset(direction);
            }
        }

        if (i != 3 || secondPos == null) return false;
        // Ignore hole with a player
        if (ignoreWithPlayer && secondPos.hasEntity(entity -> entity instanceof PlayerEntity)) return false;

        for (Direction direction : Direction.values()) {
            if (direction == Direction.UP || direction == Direction.DOWN) continue;

            if (secondPos.offset(direction).blastRes()) {
                i++;
            }
        }

        return i == 6;
    }
}