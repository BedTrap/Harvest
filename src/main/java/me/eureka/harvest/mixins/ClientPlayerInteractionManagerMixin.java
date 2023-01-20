package me.eureka.harvest.mixins;

import me.eureka.harvest.Hack;
import me.eureka.harvest.events.event.BreakBlockEvent;
import me.eureka.harvest.mixininterface.IClientPlayerInteractionManager;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin implements IClientPlayerInteractionManager {
    @Shadow
    protected abstract void syncSelectedSlot();

    @Inject(method = {"attackBlock"}, at = {@At("HEAD")}, cancellable = true)
    private void onAttackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        BreakBlockEvent event = new BreakBlockEvent(pos, direction);
        Hack.event().post(event);

        if (event.isCancelled()) cir.cancel();
    }

    @Override
    public void syncSelected() {
        syncSelectedSlot();
    }
}
