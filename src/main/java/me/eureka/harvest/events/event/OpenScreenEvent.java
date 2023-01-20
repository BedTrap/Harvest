package me.eureka.harvest.events.event;

import me.eureka.harvest.events.Cancelled;
import net.minecraft.client.gui.screen.Screen;

public class OpenScreenEvent extends Cancelled {

    public Screen screen;

    public OpenScreenEvent(Screen screen){
        this.screen = screen;
    }
}