package me.eureka.harvest.systems.utils.game;

import me.eureka.harvest.systems.utils.advanced.BlockPosX;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static me.eureka.harvest.ic.util.Wrapper.mc;

public class PlayerState {
    /**
     * @param player player position
     * @param dynamic if true, method will use another way to get all possible positions
     * @return true if player is in surround
     */
    public static boolean isSurrounded(PlayerEntity player, boolean dynamic) {
        BlockPosX start = new BlockPosX(player.getBlockPos());

        if (dynamic) {
            // doesnt work
            List<BlockPosX> positions = new ArrayList<>();
            List<BlockPosX> possible_positions = dynamicPositions(start);

            possible_positions.stream()
                    .filter((block_pos) -> !block_pos.replaceable()) // removes all non-blast resist blocks
                    .filter((block_pos) -> !block_pos.hasEntity((entity) -> entity == mc.player))
                    .forEach((block_pos) -> Stream.of(Direction.values())
                            .filter((direction) -> !direction.equals(Direction.UP))
                            .filter((direction) -> !direction.equals(Direction.DOWN))
                            .map(block_pos::offset)
                            .forEach((offset) -> {
                                if (offset.hasEntity((entity) -> entity == mc.player)) {
                                    positions.add(block_pos);
                                }
                            }));

            return positions.isEmpty(); // returns true if all blocks is blast resist
        } else {
            return Stream.of(Direction.values())
                    .filter((direction) -> !direction.equals(Direction.UP))
                    .filter((direction) -> !direction.equals(Direction.DOWN))
                    .map(start::offset)
                    .filter(BlockPosX::blastRes)
                    .count() == 4 /*blocks*/;
        }
    }

    /**
     * @param start player position
     * @return list of the possible position for dynamic surround
     */
    private static List<BlockPosX> dynamicPositions(BlockPosX start) {
        List<BlockPosX> positions = new ArrayList<>();

        for (int x = start.x() - 3; x < start.z() + 3; x++) {
            for (int y = start.y() - 1; y < start.y() + 1; y++) {
                for (int z = start.z() - 3; z < start.z() + 3; z++) {
                    BlockPosX block_pos = new BlockPosX(x, y, z);

                    if (start.distance(block_pos) <= 3) positions.add(block_pos);
                }
            }
        }

        return positions;
    }
}
