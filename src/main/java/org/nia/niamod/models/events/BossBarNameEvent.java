package org.nia.niamod.models.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

public interface BossBarNameEvent {

    Event<@NotNull BossBarNameEvent> MODIFY = EventFactory.createArrayBacked(BossBarNameEvent.class, listeners -> title -> {
        for (BossBarNameEvent listener : listeners) {
            title = listener.modify(title);
        }
        return title;
    });

    Text modify(Text title);


}
