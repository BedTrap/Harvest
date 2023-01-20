package me.eureka.harvest.events.event;

import me.eureka.harvest.events.Cancelled;
import net.minecraft.entity.Entity;

public class EntityAddedEvent extends Cancelled {

    public Entity entity;

    public EntityAddedEvent(Entity entity) {
        this.entity = entity;
    }

}
