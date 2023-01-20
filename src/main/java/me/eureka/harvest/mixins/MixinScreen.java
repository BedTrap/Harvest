package me.eureka.harvest.mixins;

import me.eureka.harvest.Hack;
import me.eureka.harvest.events.event.RenderTooltipEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.util.math.MatrixStack;

import java.util.List;

@Mixin(Screen.class)
public class MixinScreen {
	@Unique private int lastMX;
	@Unique private int lastMY;

	@Unique private boolean skipTooltip;

	@Shadow private void renderTooltipFromComponents(MatrixStack matrices, List<TooltipComponent> components, int x, int y) {}

	@Inject(method = "render", at = @At("HEAD"))
	private void render(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo callback) {
		lastMX = mouseX;
		lastMY = mouseY;
	}

	@Inject(method = "renderTooltipFromComponents", at = @At("HEAD"), cancellable = true)
	private void renderTooltipFromComponents(MatrixStack matrices, List<TooltipComponent> components, int x, int y, CallbackInfo callback) {
		if (skipTooltip) {
			skipTooltip = false;
			return;
		}

		RenderTooltipEvent event = new RenderTooltipEvent((Screen) (Object) this, matrices, components, x, y, lastMX, lastMY);
		Hack.event().post(event);

		callback.cancel();
		if (event.isCancelled()) {
			return;
		}

		skipTooltip = true;
		renderTooltipFromComponents(event.getMatrix(), event.getComponents(), event.getX(), event.getY());
	}
}