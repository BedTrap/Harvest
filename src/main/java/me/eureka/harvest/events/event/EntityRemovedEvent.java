package me.eureka.harvest.events.event;

import net.minecraft.entity.Entity;

public class EntityRemovedEvent {
    public Entity entity;

    public EntityRemovedEvent(Entity entity) {
        this.entity = entity;
    }
}
