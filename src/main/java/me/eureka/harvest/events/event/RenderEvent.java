package me.eureka.harvest.events.event;

import me.eureka.harvest.events.Cancelled;
import net.minecraft.client.util.math.MatrixStack;


public class RenderEvent extends Cancelled {

    private final MatrixStack stack;

    public RenderEvent(MatrixStack stack) {
        this.stack = stack;
    }

    public MatrixStack getMatrixStack() {
        return stack;
    }

}
