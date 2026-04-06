package org.nia.niamod.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.experimental.UtilityClass;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

@UtilityClass
public class Render3D {
    private static final int FACE_ALPHA_BOTTOM = 70;
    private static final int FACE_ALPHA_TOP = 0;
    private static final int LINE_ALPHA_BOTTOM = 250;
    private static final int LINE_ALPHA_TOP = 0;

    public static void renderBox(WorldRenderContext context, BlockPos corner1, BlockPos corner2, int r, int g, int b) {
        Render3D.box(context, corner1, corner2, (r << 16) | (g << 8) | b);
    }

    public static void box(WorldRenderContext context, BlockPos start, BlockPos end, int color) {
        if (start == null || end == null) {
            return;
        }

        drawGradientBox(context, new AABB(
                Math.min(start.getX(), end.getX()),
                Math.min(start.getY(), end.getY()),
                Math.min(start.getZ(), end.getZ()),
                Math.max(start.getX(), end.getX()) + 1.0,
                Math.max(start.getY(), end.getY()) + 1.0,
                Math.max(start.getZ(), end.getZ()) + 1.0
        ), color);
    }

    public static void drawGradientBox(WorldRenderContext context, AABB box, int color) {
        PoseStack matrices = context.matrices();
        Vec3 camera = context.worldState().cameraRenderState.pos;

        float x1 = (float) (box.minX - camera.x);
        float x2 = (float) (box.maxX - camera.x);
        float y1 = (float) (box.minY - camera.y);
        float y2 = (float) (box.maxY - camera.y);
        float z1 = (float) (box.minZ - camera.z);
        float z2 = (float) (box.maxZ - camera.z);

        int red = color >> 16 & 0xFF;
        int green = color >> 8 & 0xFF;
        int blue = color & 0xFF;

        Matrix4f matrix = matrices.last().pose();
        MultiBufferSource consumers = context.consumers();
        renderFaces(consumers.getBuffer(RenderTypes.debugQuads()), matrix, x1, y1, z1, x2, y2, z2, red, green, blue);
        renderEdges(consumers.getBuffer(RenderTypes.lines()), matrix, x1, y1, z1, x2, y2, z2, red, green, blue);
    }

    private static void renderFaces(VertexConsumer vertexConsumer, Matrix4f matrix,
                                    float x1, float y1, float z1,
                                    float x2, float y2, float z2,
                                    int red, int green, int blue) {
        quad(vertexConsumer, matrix, red, green, blue, x1, y2, z1, x2, y2, z1, x2, y1, z1, x1, y1, z1);
        quad(vertexConsumer, matrix, red, green, blue, x2, y2, z2, x1, y2, z2, x1, y1, z2, x2, y1, z2);
        quad(vertexConsumer, matrix, red, green, blue, x1, y2, z2, x1, y2, z1, x1, y1, z1, x1, y1, z2);
        quad(vertexConsumer, matrix, red, green, blue, x2, y2, z1, x2, y2, z2, x2, y1, z2, x2, y1, z1);
    }

    private static void renderEdges(VertexConsumer vertexConsumer, Matrix4f matrix,
                                    float x1, float y1, float z1,
                                    float x2, float y2, float z2,
                                    int red, int green, int blue) {
        line(vertexConsumer, matrix, red, green, blue, x1, y1, z1, LINE_ALPHA_BOTTOM, x1, y2, z1, LINE_ALPHA_TOP);
        line(vertexConsumer, matrix, red, green, blue, x2, y1, z1, LINE_ALPHA_BOTTOM, x2, y2, z1, LINE_ALPHA_TOP);
        line(vertexConsumer, matrix, red, green, blue, x1, y1, z2, LINE_ALPHA_BOTTOM, x1, y2, z2, LINE_ALPHA_TOP);
        line(vertexConsumer, matrix, red, green, blue, x2, y1, z2, LINE_ALPHA_BOTTOM, x2, y2, z2, LINE_ALPHA_TOP);
    }

    private static void quad(VertexConsumer vertexConsumer, Matrix4f matrix, int red, int green, int blue,
                             float tx, float ty, float tz,
                             float bx1, float by1, float bz1,
                             float bx2, float by2, float bz2,
                             float bx3, float by3, float bz3) {
        vertexConsumer.addVertex(matrix, tx, ty, tz).setColor(red, green, blue, FACE_ALPHA_TOP);
        vertexConsumer.addVertex(matrix, bx1, by1, bz1).setColor(red, green, blue, FACE_ALPHA_TOP);
        vertexConsumer.addVertex(matrix, bx2, by2, bz2).setColor(red, green, blue, FACE_ALPHA_BOTTOM);
        vertexConsumer.addVertex(matrix, bx3, by3, bz3).setColor(red, green, blue, FACE_ALPHA_BOTTOM);
    }

    private static void line(VertexConsumer vertexConsumer, Matrix4f matrix, int red, int green, int blue,
                             float x1, float y1, float z1, int a1,
                             float x2, float y2, float z2, int a2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length <= 0.0f) {
            return;
        }

        float nx = dx / length;
        float ny = dy / length;
        float nz = dz / length;
        vertexConsumer.addVertex(matrix, x1, y1, z1).setColor(red, green, blue, a1).setNormal(nx, ny, nz).setLineWidth(1.0f);
        vertexConsumer.addVertex(matrix, x2, y2, z2).setColor(red, green, blue, a2).setNormal(nx, ny, nz).setLineWidth(1.0f);
    }
}
