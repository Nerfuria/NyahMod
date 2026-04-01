package org.nia.niamod.models.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import org.jetbrains.annotations.NotNull;

public interface PostInitEvent {

    Event<@NotNull PostInitEvent> EVENT = EventFactory.createArrayBacked(PostInitEvent.class,
            (listeners) -> () -> {
                for (PostInitEvent listener : listeners) {
                    listener.onInit();
                }
            });

    void onInit();

}
