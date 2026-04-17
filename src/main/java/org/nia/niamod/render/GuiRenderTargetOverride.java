package org.nia.niamod.render;

import com.mojang.blaze3d.pipeline.RenderTarget;

public final class GuiRenderTargetOverride {
    private static final ThreadLocal<RenderTarget> CURRENT = new ThreadLocal<>();

    private GuiRenderTargetOverride() {
    }

    public static RenderTarget get() {
        return CURRENT.get();
    }

    public static Scope push(RenderTarget renderTarget) {
        RenderTarget previous = CURRENT.get();
        CURRENT.set(renderTarget);
        return new Scope(previous);
    }

    public static final class Scope implements AutoCloseable {
        private final RenderTarget previous;

        private Scope(RenderTarget previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }
}
