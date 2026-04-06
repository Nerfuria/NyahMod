package org.nia.niamod.render;

import lombok.experimental.UtilityClass;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.core.BlockPos;

@UtilityClass
public class BoxRenderer {
    public static void renderBox(WorldRenderContext context, BlockPos corner1, BlockPos corner2, int r, int g, int b) {
        Render3D.box(context, corner1, corner2, (r << 16) | (g << 8) | b);
    }
}
