package me.eureka.harvest.events.event;

import me.eureka.harvest.events.Cancelled;

public class SprintEvent extends Cancelled {
    private boolean sprinting;

    public SprintEvent(boolean sprinting) {
        this.sprinting = sprinting;
    }

    public boolean isSprinting() {
        return sprinting;
    }

    public SprintEvent setSprinting(boolean sprinting) {
        this.sprinting = sprinting;
        return this;
    }
}