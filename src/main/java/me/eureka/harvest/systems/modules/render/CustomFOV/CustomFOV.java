package me.eureka.harvest.systems.modules.render.CustomFOV;

import me.eureka.harvest.events.event.Render3DEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.mixininterface.ISimpleOption;
import me.eureka.harvest.systems.modules.Module;
import com.google.common.eventbus.Subscribe;
import net.minecraft.client.option.SimpleOption;

import java.util.Objects;

@Module.Info(name = "CustomFov", category = Module.Category.Render)
public class CustomFOV extends Module {
    public Setting<Integer> fov = register("Fov", 110, 100, 175);

    @Subscribe
    public void onTick(Render3DEvent event) {

        SimpleOption<Integer> newFov = mc.options.getFov();
        if (newFov.getValue() != fov.get().doubleValue()) {
            @SuppressWarnings("unchecked")
            var newFov2 = (ISimpleOption<Integer>) (Object) newFov;
            newFov2.forceSetValue(fov.get());
        }
    }
}
