package me.eureka.harvest.events.event;

import me.eureka.harvest.events.Cancelled;
import net.minecraft.particle.ParticleEffect;

public class ParticleEvent extends Cancelled {
    public ParticleEffect particle;

    public ParticleEvent(ParticleEffect particle) {
        this.setCancelled(false);
        this.particle = particle;
    }
}
