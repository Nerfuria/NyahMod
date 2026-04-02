package org.nia.niamod.mixin.wynnextras;

import julianh06.wynnextras.features.bankoverlay.BankOverlay2;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import org.nia.niamod.models.events.SlotRenderEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BankOverlay2.class)
public class BankOverlayMixin {

    @Inject(method = "renderItemOverlays", at = @At("RETURN"))
    private static void renderOverlay(DrawContext context, ItemStack stack, int x, int y, CallbackInfo ci) {
        SlotRenderEvent.EVENT.invoker().render(context, stack, x, y);
    }

}
