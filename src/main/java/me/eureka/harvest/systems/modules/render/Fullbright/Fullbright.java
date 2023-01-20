package me.eureka.harvest.systems.modules.render.Fullbright;

import me.eureka.harvest.events.event.TickEvent;
import me.eureka.harvest.mixininterface.ISimpleOption;
import me.eureka.harvest.systems.modules.Module;
import com.google.common.eventbus.Subscribe;
import net.minecraft.client.option.SimpleOption;

@Module.Info(name = "Fullbright", category = Module.Category.Render)
public class Fullbright extends Module {

    @Subscribe
    public void onTick(TickEvent.Post event) {
        SimpleOption<Double> newGamma = mc.options.getGamma();
        if (newGamma.getValue() != 420.0) {
            @SuppressWarnings("unchecked")
            var newGamma2 = (ISimpleOption<Double>) (Object) newGamma;
            newGamma2.forceSetValue(420.0);
        }
    }
}
