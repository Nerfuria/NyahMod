package org.nia.niamod.render;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class BoxRenderer {

    private static final int ALPHA_FACE_BOTTOM = 70;
    private static final int ALPHA_FACE_TOP = 0;
    private static final int ALPHA_LINE_BOTTOM = 250;
    private static final int ALPHA_LINE_TOP = 0;

    public static void renderBox(WorldRenderContext context, BlockPos corner1, BlockPos corner2, int r, int g, int b) {
        if (corner1 == null || corner2 == null) return;

        MatrixStack matrices = context.matrices();
        Vec3d cam = context.worldState().cameraRenderState.pos;

        float x1 = (float) (Math.min(corner1.getX(), corner2.getX()) - cam.x);
        float x2 = (float) (Math.max(corner1.getX(), corner2.getX()) + 1.0 - cam.x);
        float y1 = (float) (Math.min(corner1.getY(), corner2.getY()) - cam.y);
        float y2 = (float) (Math.max(corner1.getY(), corner2.getY()) + 1.0 - cam.y);
        float z1 = (float) (Math.min(corner1.getZ(), corner2.getZ()) - cam.z);
        float z2 = (float) (Math.max(corner1.getZ(), corner2.getZ()) + 1.0 - cam.z);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumerProvider consumers = context.consumers();

        renderFaces(consumers.getBuffer(RenderLayers.debugQuads()), matrix, x1, y1, z1, x2, y2, z2, r, g, b);
        renderEdges(consumers.getBuffer(RenderLayers.lines()), matrix, x1, y1, z1, x2, y2, z2, r, g, b);
    }

    private static void renderFaces(VertexConsumer vc, Matrix4f m,
                                    float x1, float y1, float z1,
                                    float x2, float y2, float z2,
                                    int r, int g, int b) {
        quad(vc, m, r, g, b, x1, y2, z1, x2, y2, z1, x2, y1, z1, x1, y1, z1);
        quad(vc, m, r, g, b, x2, y2, z2, x1, y2, z2, x1, y1, z2, x2, y1, z2);
        quad(vc, m, r, g, b, x1, y2, z2, x1, y2, z1, x1, y1, z1, x1, y1, z2);
        quad(vc, m, r, g, b, x2, y2, z1, x2, y2, z2, x2, y1, z2, x2, y1, z1);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, int r, int g, int b,
                             float tx, float ty, float tz,
                             float bx1, float by1, float bz1,
                             float bx2, float by2, float bz2,
                             float bx3, float by3, float bz3) {
        vc.vertex(m, tx, ty, tz).color(r, g, b, ALPHA_FACE_TOP);
        vc.vertex(m, bx1, by1, bz1).color(r, g, b, ALPHA_FACE_TOP);
        vc.vertex(m, bx2, by2, bz2).color(r, g, b, ALPHA_FACE_BOTTOM);
        vc.vertex(m, bx3, by3, bz3).color(r, g, b, ALPHA_FACE_BOTTOM);
    }

    private static void renderEdges(VertexConsumer lc, Matrix4f m,
                                    float x1, float y1, float z1,
                                    float x2, float y2, float z2,
                                    int r, int g, int b) {
        line(lc, m, r, g, b, x1, y1, z1, ALPHA_LINE_BOTTOM, x1, y2, z1, ALPHA_LINE_TOP);
        line(lc, m, r, g, b, x2, y1, z1, ALPHA_LINE_BOTTOM, x2, y2, z1, ALPHA_LINE_TOP);
        line(lc, m, r, g, b, x1, y1, z2, ALPHA_LINE_BOTTOM, x1, y2, z2, ALPHA_LINE_TOP);
        line(lc, m, r, g, b, x2, y1, z2, ALPHA_LINE_BOTTOM, x2, y2, z2, ALPHA_LINE_TOP);
    }

    private static void line(VertexConsumer lc, Matrix4f m, int r, int g, int b,
                             float x0, float y0, float z0, int a0,
                             float x1, float y1, float z1, int a1) {
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len == 0) return;
        float nx = dx / len, ny = dy / len, nz = dz / len;
        lc.vertex(m, x0, y0, z0).color(r, g, b, a0).normal(nx, ny, nz).lineWidth(1.0f);
        lc.vertex(m, x1, y1, z1).color(r, g, b, a1).normal(nx, ny, nz).lineWidth(1.0f);
    }
}