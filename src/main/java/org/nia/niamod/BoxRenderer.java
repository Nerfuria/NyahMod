package org.nia.niamod;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.nia.niamod.config.NyahConfig;

public class BoxRenderer {

    public static void renderBox(WorldRenderContext context, BlockPos corner1, BlockPos corner2) {
        if (corner1 == null || corner2 == null) return;

        MatrixStack matrices = context.matrices();
        Vec3d camPos = context.worldState().cameraRenderState.pos;

        double minX = Math.min(corner1.getX(), corner2.getX());
        double minY = Math.min(corner1.getY(), corner2.getY());
        double minZ = Math.min(corner1.getZ(), corner2.getZ());
        double maxX = Math.max(corner1.getX(), corner2.getX()) + 1.0;
        double maxY = Math.max(corner1.getY(), corner2.getY()) + 1.0;
        double maxZ = Math.max(corner1.getZ(), corner2.getZ()) + 1.0;

        float bx1 = (float)(minX - camPos.x);
        float bx2 = (float)(maxX - camPos.x);
        float by1 = (float)(minY - camPos.y);
        float by2 = (float)(maxY - camPos.y);
        float bz1 = (float)(minZ - camPos.z);
        float bz2 = (float)(maxZ - camPos.z);

        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) return;

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        int r = NyahConfig.nyahConfigData.color >> 16 & 0xFF, g = NyahConfig.nyahConfigData.color >> 8 & 0xFF, b = NyahConfig.nyahConfigData.color & 0xFF;
        int alphaBottom = 70; // 50%
        int alphaTop    = 0;   // 0%
        int lineAlphaBottom = 170;
        int lineAlphaTop    = 0;

        VertexConsumer vc = consumers.getBuffer(RenderLayers.debugQuads());

        vc.vertex(matrix, bx1, by2, bz1).color(r, g, b, alphaTop);
        vc.vertex(matrix, bx2, by2, bz1).color(r, g, b, alphaTop);
        vc.vertex(matrix, bx2, by1, bz1).color(r, g, b, alphaBottom);
        vc.vertex(matrix, bx1, by1, bz1).color(r, g, b, alphaBottom);

        vc.vertex(matrix, bx2, by2, bz2).color(r, g, b, alphaTop);
        vc.vertex(matrix, bx1, by2, bz2).color(r, g, b, alphaTop);
        vc.vertex(matrix, bx1, by1, bz2).color(r, g, b, alphaBottom);
        vc.vertex(matrix, bx2, by1, bz2).color(r, g, b, alphaBottom);

        vc.vertex(matrix, bx1, by2, bz2).color(r, g, b, alphaTop);
        vc.vertex(matrix, bx1, by2, bz1).color(r, g, b, alphaTop);
        vc.vertex(matrix, bx1, by1, bz1).color(r, g, b, alphaBottom);
        vc.vertex(matrix, bx1, by1, bz2).color(r, g, b, alphaBottom);

        vc.vertex(matrix, bx2, by2, bz1).color(r, g, b, alphaTop);
        vc.vertex(matrix, bx2, by2, bz2).color(r, g, b, alphaTop);
        vc.vertex(matrix, bx2, by1, bz2).color(r, g, b, alphaBottom);
        vc.vertex(matrix, bx2, by1, bz1).color(r, g, b, alphaBottom);

        VertexConsumer lc = consumers.getBuffer(RenderLayers.lines());

        line(lc, matrix, bx1, by1, bz1, lineAlphaBottom, bx1, by2, bz1, lineAlphaTop, r, g, b);
        line(lc, matrix, bx2, by1, bz1, lineAlphaBottom, bx2, by2, bz1, lineAlphaTop, r, g, b);
        line(lc, matrix, bx1, by1, bz2, lineAlphaBottom, bx1, by2, bz2, lineAlphaTop, r, g, b);
        line(lc, matrix, bx2, by1, bz2, lineAlphaBottom, bx2, by2, bz2, lineAlphaTop, r, g, b);

        line(lc, matrix, bx1, by1, bz1, lineAlphaBottom, bx2, by1, bz1, lineAlphaBottom, r, g, b);
        line(lc, matrix, bx2, by1, bz1, lineAlphaBottom, bx2, by1, bz2, lineAlphaBottom, r, g, b);
        line(lc, matrix, bx2, by1, bz2, lineAlphaBottom, bx1, by1, bz2, lineAlphaBottom, r, g, b);
        line(lc, matrix, bx1, by1, bz2, lineAlphaBottom, bx1, by1, bz1, lineAlphaBottom, r, g, b);
    }

    private static void line(VertexConsumer lc, Matrix4f matrix,
                             float x0, float y0, float z0, int a0,
                             float x1, float y1, float z1, int a1,
                             int r, int g, int b) {
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        float len = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (len == 0) return;
        float nx = dx/len, ny = dy/len, nz = dz/len;
        lc.vertex(matrix, x0, y0, z0).color(r, g, b, a0).normal(nx, ny, nz).lineWidth(1.0f);
        lc.vertex(matrix, x1, y1, z1).color(r, g, b, a1).normal(nx, ny, nz).lineWidth(1.0f);
    }
}