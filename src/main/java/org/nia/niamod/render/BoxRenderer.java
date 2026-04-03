package org.nia.niamod.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.experimental.UtilityClass;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

@UtilityClass
public class BoxRenderer {
    private static final int ALPHA_FACE_BOTTOM = 70;
    private static final int ALPHA_FACE_TOP = 0;
    private static final int ALPHA_LINE_BOTTOM = 250;
    private static final int ALPHA_LINE_TOP = 0;

    public static void renderBox(WorldRenderContext context, BlockPos corner1, BlockPos corner2, int r, int g, int b) {
        if (corner1 == null || corner2 == null) return;

        PoseStack matrices = context.matrices();
        Vec3 cam = context.worldState().cameraRenderState.pos;

        float x1 = (float) (Math.min(corner1.getX(), corner2.getX()) - cam.x);
        float x2 = (float) (Math.max(corner1.getX(), corner2.getX()) + 1.0 - cam.x);
        float y1 = (float) (Math.min(corner1.getY(), corner2.getY()) - cam.y);
        float y2 = (float) (Math.max(corner1.getY(), corner2.getY()) + 1.0 - cam.y);
        float z1 = (float) (Math.min(corner1.getZ(), corner2.getZ()) - cam.z);
        float z2 = (float) (Math.max(corner1.getZ(), corner2.getZ()) + 1.0 - cam.z);

        Matrix4f matrix = matrices.last().pose();
        MultiBufferSource consumers = context.consumers();

        renderFaces(consumers.getBuffer(RenderTypes.debugQuads()), matrix, x1, y1, z1, x2, y2, z2, r, g, b);
        renderEdges(consumers.getBuffer(RenderTypes.lines()), matrix, x1, y1, z1, x2, y2, z2, r, g, b);
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
        vc.addVertex(m, tx, ty, tz).setColor(r, g, b, ALPHA_FACE_TOP);
        vc.addVertex(m, bx1, by1, bz1).setColor(r, g, b, ALPHA_FACE_TOP);
        vc.addVertex(m, bx2, by2, bz2).setColor(r, g, b, ALPHA_FACE_BOTTOM);
        vc.addVertex(m, bx3, by3, bz3).setColor(r, g, b, ALPHA_FACE_BOTTOM);
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
        lc.addVertex(m, x0, y0, z0).setColor(r, g, b, a0).setNormal(nx, ny, nz).setLineWidth(1.0f);
        lc.addVertex(m, x1, y1, z1).setColor(r, g, b, a1).setNormal(nx, ny, nz).setLineWidth(1.0f);
    }
}
