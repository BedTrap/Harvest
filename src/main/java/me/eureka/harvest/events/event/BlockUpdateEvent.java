package me.eureka.harvest.events.event;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class BlockUpdateEvent {
    public BlockPos pos;
    public BlockState oldState, newState;

    public BlockUpdateEvent(BlockPos pos, BlockState oldState, BlockState newState) {
        this.pos = pos;
        this.oldState = oldState;
        this.newState = newState;
    }
}