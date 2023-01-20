package me.eureka.harvest.systems.modules.render.NoRender;

import me.eureka.harvest.events.event.ParticleEvent;
import me.eureka.harvest.events.event.TickEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.systems.modules.Module;
import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;

@Module.Info(name = "NoRender", category = Module.Category.Render)
public class NoRender extends Module {
    public Setting<Boolean> explosions = register("Explosions", true);
    public Setting<Boolean> rockets = register("Rockets", false);
    public Setting<Boolean> fog = register("Fog", true);
    public Setting<Boolean> hurt = register("Hurt", true);
    public Setting<Boolean> pop = register("Pop", true);
    public Setting<Boolean> potionIcons = register("PotionIcon", true);
    public Setting<Boolean> particles = register("AllParticles", false);
    public Setting<Boolean> nausBling = register("Nausea&Blind", true);
    public Setting<Boolean> noFireOverlay = register("FireOverlay", true);
    public Setting<Boolean> noInBlockOverlay = register("BlockOverlay", true);
    public Setting<Boolean> noUnderWaterOverlay = register("WaterOverlay", true);

    @Subscribe
    public void onParticle(ParticleEvent event) {
        if (explosions.get() && event.particle == ParticleTypes.EXPLOSION) event.cancel();
        if (rockets.get() && event.particle == ParticleTypes.FIREWORK) event.cancel();
        if (particles.get()) event.cancel();
    }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        if (nausBling.get() && mc.player != null && mc.player.hasStatusEffect(StatusEffects.BLINDNESS) || mc.player.hasStatusEffect(StatusEffects.NAUSEA)) {
            mc.player.removeStatusEffectInternal(StatusEffects.NAUSEA);
            mc.player.removeStatusEffectInternal(StatusEffects.BLINDNESS);

        }
    }

    public static NoRender get;

    public NoRender() {
        get = this;
    }
}
