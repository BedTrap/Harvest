package me.eureka.harvest.mixins;

import me.eureka.harvest.Hack;
import me.eureka.harvest.events.event.PlaceBlockEvent;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class MixinBlockItem {
    @Inject(method = "place(Lnet/minecraft/item/ItemPlacementContext;Lnet/minecraft/block/BlockState;)Z", at = @At("HEAD"), cancellable = true)
    private void onPlace(ItemPlacementContext context, BlockState state, CallbackInfoReturnable<Boolean> info) {
        if (context.getWorld().isClient()) {
            BlockHitResult hitResult = new BlockHitResult(context.getHitPos(), context.getSide(), context.getBlockPos(), context.hitsInsideBlock());

            PlaceBlockEvent event = new PlaceBlockEvent(hitResult, context.getStack(), context.getHand());
            Hack.event().post(event);
            if (event.isCancelled()) info.cancel();
        }
    }
}
