package me.eureka.harvest.events.event;

import me.eureka.harvest.events.Cancelled;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class BreakBlockEvent extends Cancelled {
    public BlockPos blockPos;
    public Direction direction;

    public BreakBlockEvent(BlockPos blockPos, Direction direction) {
        this.blockPos = blockPos;
        this.direction = direction;
    }
}