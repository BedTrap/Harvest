package me.eureka.harvest.events.event;

import me.eureka.harvest.events.Cancelled;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.util.math.MatrixStack;

import java.util.List;

public class RenderTooltipEvent extends Cancelled {

	private Screen screen;
	private MatrixStack matrices;
	private List<TooltipComponent> components;
	private int x;
	private int y;
	private int mouseX;
	private int mouseY;

	public RenderTooltipEvent(Screen screen, MatrixStack matrices, List<TooltipComponent> components, int x, int y, int mouseX, int mouseY) {
		this.matrices = matrices;
		this.screen = screen;
		this.components = components;
		this.x = x;
		this.y = y;
		this.mouseX = mouseX;
		this.mouseY = mouseY;
	}
	
	public Screen getScreen() {
		return screen;
	}

	public MatrixStack getMatrix() {
		return matrices;
	}

	public void setMatrix(MatrixStack matrices) {
		this.matrices = matrices;
	}

	public List<TooltipComponent> getComponents() {
		return components;
	}

	public void setComponents(List<TooltipComponent> components) {
		this.components = components;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getMouseX() {
		return mouseX;
	}

	public int getMouseY() {
		return mouseY;
	}
	
}