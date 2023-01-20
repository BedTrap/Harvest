package me.eureka.harvest.mixins;

import net.minecraft.world.entity.EntityLike;
import net.minecraft.world.entity.SectionedEntityCache;
import net.minecraft.world.entity.SimpleEntityLookup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SimpleEntityLookup.class)
public interface ISimpleEntityLookupAccessor {
    @Accessor("cache")
    <T extends EntityLike> SectionedEntityCache<T> getCache();
}