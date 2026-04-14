package org.nia.niamod.models.events;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import org.nia.niamod.eventbus.Cancelable;

public record CommandSentEvent(String command) implements Cancelable {
}
