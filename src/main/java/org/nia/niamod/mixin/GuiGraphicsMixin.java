package org.nia.niamod.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.nia.niamod.render.GuiGraphicsScissorState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.Deque;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin implements GuiGraphicsScissorState {
    @Unique
    private final Deque<ScreenRectangle> niamod$scissorAreas = new ArrayDeque<>();

    @Inject(method = "enableScissor", at = @At("TAIL"))
    private void niamod$trackEnableScissor(int x1, int y1, int x2, int y2, CallbackInfo ci) {
        ScreenRectangle requested = new ScreenRectangle(
                x1,
                y1,
                Math.max(0, x2 - x1),
                Math.max(0, y2 - y1)
        );
        ScreenRectangle current = niamod$scissorAreas.peekLast();
        ScreenRectangle next = current == null ? requested : current.intersection(requested);
        niamod$scissorAreas.addLast(next == null ? ScreenRectangle.empty() : next);
    }

    @Inject(method = "disableScissor", at = @At("TAIL"))
    private void niamod$trackDisableScissor(CallbackInfo ci) {
        if (!niamod$scissorAreas.isEmpty()) {
            niamod$scissorAreas.removeLast();
        }
    }

    @Override
    public ScreenRectangle niamod$getCurrentScissorArea() {
        return niamod$scissorAreas.peekLast();
    }
}
