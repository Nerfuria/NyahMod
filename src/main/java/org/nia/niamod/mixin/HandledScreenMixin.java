package org.nia.niamod.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.nia.niamod.models.events.SlotRenderEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public class HandledScreenMixin {

    @Inject(method = "drawSlot", at = @At("RETURN"))
    public void renderSlot(DrawContext context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        SlotRenderEvent.EVENT.invoker().render(context, slot.getStack(), slot.x, slot.y);
    }

}
