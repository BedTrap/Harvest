package me.eureka.harvest.mixins;

import me.eureka.harvest.Hack;
import me.eureka.harvest.events.event.EventBlockShape;
import me.eureka.harvest.systems.utils.advanced.BlockPosX;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockCollisionSpliterator;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockCollisionSpliterator.class)
public class MixinBlockCollisionSpliterator {
	@Redirect(method = "computeNext*", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;getCollisionShape(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/ShapeContext;)Lnet/minecraft/util/shape/VoxelShape;"))
	private VoxelShape computeNext_getCollisionShape(BlockState blockState, BlockView world, BlockPos pos, ShapeContext context) {
		VoxelShape shape = blockState.getCollisionShape(world, pos, context);
		EventBlockShape event = new EventBlockShape(new BlockPosX(pos), shape);
		Hack.event().post(event);

		if (event.isCancelled()) {
			return VoxelShapes.empty();
		}

		return event.shape;
	}
}