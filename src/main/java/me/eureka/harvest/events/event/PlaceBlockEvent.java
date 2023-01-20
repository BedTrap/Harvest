package me.eureka.harvest.events.event;

import me.eureka.harvest.events.Cancelled;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;

public class PlaceBlockEvent extends Cancelled {
    public BlockHitResult hitResult;
    public ItemStack itemStack;
    public Hand hand;

    public PlaceBlockEvent(BlockHitResult hitResult, ItemStack itemStack, Hand hand) {
        this.hitResult = hitResult;
        this.itemStack = itemStack;
        this.hand = hand;
    }
}
