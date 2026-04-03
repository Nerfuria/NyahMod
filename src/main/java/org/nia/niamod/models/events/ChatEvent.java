package org.nia.niamod.models.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public interface ChatEvent {

    Event<@NotNull Modify> MODIFY = EventFactory.createArrayBacked(Modify.class, listeners -> title -> {
        for (Modify listener : listeners) {
            title = listener.modifyMessage(title);
        }

        return title;
    });

    Event<@NotNull Recieved> RECIEVED = EventFactory.createArrayBacked(Recieved.class, listeners -> message -> {
        for (Recieved listener : listeners) {
            listener.onMessage(message);
        }
    });

    @FunctionalInterface
    interface Recieved {
        void onMessage(Component message);
    }

    @FunctionalInterface
    interface Modify {
        Component modifyMessage(Component message);
    }
}
