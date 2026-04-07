package org.nia.niamod.render;

import com.mojang.blaze3d.pipeline.RenderTarget;

public final class NiaRenderTarget extends RenderTarget implements AutoCloseable {
    public NiaRenderTarget(String label, int width, int height, boolean useDepth) {
        super(label, useDepth);
        createBuffers(width, height);
    }

    @Override
    public void close() {
        destroyBuffers();
    }
}
