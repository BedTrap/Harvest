package me.eureka.harvest.ic.util;

import com.mojang.blaze3d.systems.RenderSystem;
import me.eureka.harvest.events.event.Render3DEvent;
import me.eureka.harvest.systems.utils.advanced.BlockPosX;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

import java.awt.*;

public class Draw3D {
    public static Vec3d interpolationPosition(Entity entity, double partial) {
        double x = entity.prevX + ((entity.getX() - entity.prevX) * partial) - MinecraftClient.getInstance().getEntityRenderDispatcher().camera.getPos().x;
        double y = entity.prevY + ((entity.getY() - entity.prevY) * partial) - MinecraftClient.getInstance().getEntityRenderDispatcher().camera.getPos().y;
        double z = entity.prevZ + ((entity.getZ() - entity.prevZ) * partial) - MinecraftClient.getInstance().getEntityRenderDispatcher().camera.getPos().z;
        return new Vec3d(x, y, z);
    }

    public static Vec3d renderPosition(BlockPosX bp) {
        return renderPosition(bp.vec3d());
    }

    public static Vec3d renderPosition(Vec3d vec3d) {
        return renderPosition(vec3d.x, vec3d.y, vec3d.z);
    }

    public static Vec3d renderPosition(double x, double y, double z) {
        double minX = x - MinecraftClient.getInstance().getEntityRenderDispatcher().camera.getPos().x;
        double minY = y - MinecraftClient.getInstance().getEntityRenderDispatcher().camera.getPos().y;
        double minZ = z - MinecraftClient.getInstance().getEntityRenderDispatcher().camera.getPos().z;
        return new Vec3d(minX, minY, minZ);
    }

    public static void drawBox(Render3DEvent event, Box box, Color color) {
        drawOutlineBox(event, box, color);
        drawFilledBox(event, box, color);
    }

    public static void drawOutlineBox(Render3DEvent event, Box box, Color color) {
        Matrix4f matrix4f = event.matrix().peek().getPositionMatrix();

        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        VoxelShape shape = VoxelShapes.cuboid(box);
        shape.forEachEdge((x1, y1, z1, x2, y2, z2) -> {
            bufferBuilder.vertex(matrix4f, (float) x1, (float) y1, (float) z1).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).next();
            bufferBuilder.vertex(matrix4f, (float) x2, (float) y2, (float) z2).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).next();
        });

        BufferRenderer.drawWithShader(bufferBuilder.end());
    }

    public static void drawFilledBox(Render3DEvent event, Box box, Color color) {
        setup3DRender();
        
        Matrix4f matrix4f = event.matrix().peek().getPositionMatrix();

        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;

        bufferBuilder.vertex(matrix4f, minX, minY, minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).next();
        bufferBuilder.vertex(matrix4f, maxX, minY, minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).next();
        bufferBuilder.vertex(matrix4f, maxX, minY, maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).next();
        bufferBuilder.vertex(matrix4f, minX, minY, maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).next();

        bufferBuilder.vertex(matrix4f, minX, maxY, minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).next();
        bufferBuilder.vertex(matrix4f, minX, maxY, maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).next();
        bufferBuilder.vertex(matrix4f, maxX, maxY, maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).next();
        bufferBuilder.vertex(matrix4f, maxX, maxY, minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).next();

        bufferBuilder.vertex(matrix4f, minX, minY, minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).next();
        bufferBuilder.vertex(matrix4f, minX, maxY, minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).next();
        bufferBuilder.vertex(matrix4f, maxX, maxY, minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).next();
        bufferBuilder.vertex(matrix4f, maxX, minY, minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).next();

        bufferBuilder.vertex(matrix4f, maxX, minY, minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).next();
        bufferBuilder.vertex(matrix4f, maxX, maxY, minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).next();
        bufferBuilder.vertex(matrix4f, maxX, maxY, maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).next();
        bufferBuilder.vertex(matrix4f, maxX, minY, maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).next();

        bufferBuilder.vertex(matrix4f, minX, minY, maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).next();
        bufferBuilder.vertex(matrix4f, maxX, minY, maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).next();
        bufferBuilder.vertex(matrix4f, maxX, maxY, maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).next();
        bufferBuilder.vertex(matrix4f, minX, maxY, maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).next();

        bufferBuilder.vertex(matrix4f, minX, minY, minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).next();
        bufferBuilder.vertex(matrix4f, minX, minY, maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).next();
        bufferBuilder.vertex(matrix4f, minX, maxY, maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).next();
        bufferBuilder.vertex(matrix4f, minX, maxY, minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).next();
        BufferRenderer.drawWithShader(bufferBuilder.end());
        
        end3DRender();
    }

    private static void setup3DRender() {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(MinecraftClient.isFabulousGraphicsOrBetter());
        RenderSystem.enableCull();
    }

    private static void end3DRender() {
        RenderSystem.enableTexture();
        RenderSystem.disableCull();
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }
}
