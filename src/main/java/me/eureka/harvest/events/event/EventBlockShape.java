package me.eureka.harvest.events.event;

import me.eureka.harvest.events.Cancelled;
import me.eureka.harvest.systems.utils.advanced.BlockPosX;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;

public class EventBlockShape extends Cancelled {
	public BlockPosX bp;
	public VoxelShape shape;

	public EventBlockShape(BlockPosX bp, VoxelShape shape) {
		this.bp = bp;
		this.shape = shape;
	}
}
