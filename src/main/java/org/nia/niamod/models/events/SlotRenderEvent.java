package org.nia.niamod.models.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import org.jetbrains.annotations.NotNull;

public interface SlotRenderEvent {

    Event<@NotNull SlotRenderEvent> EVENT = EventFactory.createArrayBacked(SlotRenderEvent.class,
            (listeners) -> (drawContext, itemStack, slotX, slotY) -> {
                for (SlotRenderEvent listener : listeners) {
                    ActionResult result = listener.render(drawContext, itemStack, slotX, slotY);
                    if (result != ActionResult.PASS) {
                        return result;
                    }
                }
                return ActionResult.PASS;
            });

    ActionResult render(DrawContext context, ItemStack stack, int slotX, int slotY);

}
