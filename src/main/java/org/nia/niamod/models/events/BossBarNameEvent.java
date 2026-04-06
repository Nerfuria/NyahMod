package org.nia.niamod.models.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.nia.niamod.eventbus.EventInfo;
import org.nia.niamod.eventbus.Preference;
import net.minecraft.network.chat.Component;

@Getter
@Setter
@AllArgsConstructor
@EventInfo(preference = Preference.CALLER)
public class BossBarNameEvent {
    private Component title;
}
