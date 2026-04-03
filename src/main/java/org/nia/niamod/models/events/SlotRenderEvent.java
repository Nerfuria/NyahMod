package org.nia.niamod.models.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface SlotRenderEvent {

    Event<@NotNull SlotRenderEvent> EVENT = EventFactory.createArrayBacked(SlotRenderEvent.class,
            (listeners) -> (drawContext, itemStack, slotX, slotY) -> {
                for (SlotRenderEvent listener : listeners) {
                    listener.render(drawContext, itemStack, slotX, slotY);
                }
            });

    void render(GuiGraphics context, ItemStack stack, int slotX, int slotY);

}
