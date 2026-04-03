package org.nia.niamod.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.nia.niamod.eventbus.NiaEventBus;
import org.nia.niamod.models.events.SlotRenderEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public class HandledScreenMixin {

    @Inject(method = "renderSlot", at = @At("RETURN"))
    public void renderSlot(GuiGraphics context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        NiaEventBus.dispatch(new SlotRenderEvent(context, slot.getItem(), slot.x, slot.y));
    }

}
