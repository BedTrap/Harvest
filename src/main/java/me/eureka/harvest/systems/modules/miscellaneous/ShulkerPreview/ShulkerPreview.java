package me.eureka.harvest.systems.modules.miscellaneous.ShulkerPreview;

import me.eureka.harvest.events.event.RenderTooltipEvent;
import me.eureka.harvest.mixins.HandledScreenAccessor;
import me.eureka.harvest.systems.modules.Module;
import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.*;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.Matrix4f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Module.Info(name = "ShulkerPreview", category = Module.Category.Miscellaneous)
public class ShulkerPreview extends Module {
    private int slotX = -1;
    private int slotY = -1;

    @Subscribe
    public void drawScreen(RenderTooltipEvent event) {
        if (!(event.getScreen() instanceof HandledScreen)) {
            return;
        }

        Slot slot = ((HandledScreenAccessor) event.getScreen()).getFocusedSlot();
        if (slot == null)
            return;

        if (slot.x != slotX || slot.y != slotY) {
            slotX = slot.x;
            slotY = slot.y;
        }

        event.getMatrix().push();
        event.getMatrix().translate(0, 0, 400);

        List<TooltipComponent> components = drawShulkerToolTip(event.getMatrix(), slot, event.getMouseX(), event.getMouseY());
        if (components != null) {
            if (components.isEmpty()) {
                event.setCancelled(true);
            } else {
                event.setComponents(components);
            }
        }

        event.getMatrix().pop();
    }

    public List<TooltipComponent> drawShulkerToolTip(MatrixStack matrices, Slot slot, int mouseX, int mouseY) {
        if (!(slot.getStack().getItem() instanceof BlockItem)) {
            return null;
        }

        Block block = ((BlockItem) slot.getStack().getItem()).getBlock();

        if (!(block instanceof ShulkerBoxBlock)
                && !(block instanceof ChestBlock)
                && !(block instanceof BarrelBlock)
                && !(block instanceof DispenserBlock)
                && !(block instanceof HopperBlock)
                && !(block instanceof AbstractFurnaceBlock)) {
            return null;
        }

        List<ItemStack> items = getItemsInContainer(slot.getStack());

        if (items.stream().allMatch(ItemStack::isEmpty)) {
            return null;
        }

        int realY = mouseY + 24;
        int tooltipWidth = block instanceof AbstractFurnaceBlock ? 47 : block instanceof HopperBlock ? 82 : 150;
        int tooltipHeight = block instanceof AbstractFurnaceBlock || block instanceof HopperBlock || block instanceof DispenserBlock ? 13 : 47;

        renderTooltipBox(matrices, mouseX, realY - tooltipHeight - 7, tooltipWidth, tooltipHeight, true);

        int count = block instanceof HopperBlock || block instanceof DispenserBlock || block instanceof AbstractFurnaceBlock ? 18 : 0;

        for (ItemStack i : items) {
            if (count > 26) {
                break;
            }

            int x = mouseX + 11 + 17 * (count % 9);
            int y = realY - 67 + 17 * (count / 9);

            mc.getItemRenderer().zOffset = 400;
            mc.getItemRenderer().renderGuiItemIcon(i, x, y);
            mc.getItemRenderer().renderGuiItemOverlay(mc.textRenderer, i, x, y, null);
            mc.getItemRenderer().zOffset = 300;
            count++;
        }

        return List.of();
    }

    private void renderTooltipBox(MatrixStack matrices, int x1, int y1, int x2, int y2, boolean wrap) {
        int xStart = x1 + 12;
        int yStart = y1 - 12;
        if (wrap) {
            if (xStart + x2 > mc.currentScreen.width)
                xStart -= 28 + x2;
            if (yStart + y2 + 6 > mc.currentScreen.height)
                yStart = mc.currentScreen.height - y2 - 6;
        }

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        fillGradient(matrix4f, bufferBuilder, xStart - 3, yStart - 4, xStart + x2 + 3, yStart - 3, -267386864, -267386864);
        fillGradient(matrix4f, bufferBuilder, xStart - 3, yStart + y2 + 3, xStart + x2 + 3, yStart + y2 + 4, -267386864, -267386864);
        fillGradient(matrix4f, bufferBuilder, xStart - 3, yStart - 3, xStart + x2 + 3, yStart + y2 + 3, -267386864, -267386864);
        fillGradient(matrix4f, bufferBuilder, xStart - 4, yStart - 3, xStart - 3, yStart + y2 + 3, -267386864, -267386864);
        fillGradient(matrix4f, bufferBuilder, xStart + x2 + 3, yStart - 3, xStart + x2 + 4, yStart + y2 + 3, -267386864, -267386864);
        fillGradient(matrix4f, bufferBuilder, xStart - 3, yStart - 3 + 1, xStart - 3 + 1, yStart + y2 + 3 - 1, 1347420415, 1344798847);
        fillGradient(matrix4f, bufferBuilder, xStart + x2 + 2, yStart - 3 + 1, xStart + x2 + 3, yStart + y2 + 3 - 1, 1347420415, 1344798847);
        fillGradient(matrix4f, bufferBuilder, xStart - 3, yStart - 3, xStart + x2 + 3, yStart - 3 + 1, 1347420415, 1347420415);
        fillGradient(matrix4f, bufferBuilder, xStart - 3, yStart + y2 + 2, xStart + x2 + 3, yStart + y2 + 3, 1344798847, 1344798847);

        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferRenderer.drawWithShader(bufferBuilder.end());
        RenderSystem.disableBlend();
        RenderSystem.enableTexture();
    }

    private void fillGradient(Matrix4f matrices, BufferBuilder bufferBuilder, int xStart, int yStart, int xEnd, int yEnd, int colorStart, int colorEnd) {
        float f = (float)(colorStart >> 24 & 255) / 255.0F;
        float g = (float)(colorStart >> 16 & 255) / 255.0F;
        float h = (float)(colorStart >> 8 & 255) / 255.0F;
        float i = (float)(colorStart & 255) / 255.0F;
        float j = (float)(colorEnd >> 24 & 255) / 255.0F;
        float k = (float)(colorEnd >> 16 & 255) / 255.0F;
        float l = (float)(colorEnd >> 8 & 255) / 255.0F;
        float m = (float)(colorEnd & 255) / 255.0F;
        bufferBuilder.vertex(matrices, (float) xEnd, (float) yStart, 0f).color(g, h, i, f).next();
        bufferBuilder.vertex(matrices, (float) xStart, (float) yStart, 0f).color(g, h, i, f).next();
        bufferBuilder.vertex(matrices, (float) xStart, (float) yEnd, 0f).color(k, l, m, j).next();
        bufferBuilder.vertex(matrices, (float) xEnd, (float) yEnd, 0f).color(k, l, m, j).next();
    }

    public List<ItemStack> getItemsInContainer(ItemStack item) {
        List<ItemStack> items = new ArrayList<>(Collections.nCopies(27, new ItemStack(Items.AIR)));
        NbtCompound nbt = item.getOrCreateNbt().contains("BlockEntityTag", 10)
                ? item.getNbt().getCompound("BlockEntityTag") : item.getNbt();

        if (nbt.contains("Items", 9)) {
            NbtList nbt2 = nbt.getList("Items", 10);
            for (int i = 0; i < nbt2.size(); i++) {
                int slot = nbt2.getCompound(i).contains("Slot", 99) ? nbt2.getCompound(i).getByte("Slot") : i;
                items.set(slot, ItemStack.fromNbt(nbt2.getCompound(i)));
            }
        }

        return items;
    }
}
