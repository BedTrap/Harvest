package me.eureka.harvest.mixins;

import net.minecraft.util.collection.TypeFilterableList;
import net.minecraft.world.entity.EntityTrackingSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityTrackingSection.class)
public interface IEntityTrackingSectionAccessor {
    @Accessor("collection")
    <T> TypeFilterableList<T> getCollection();
}