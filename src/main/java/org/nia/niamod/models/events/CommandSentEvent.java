package org.nia.niamod.models.events;

import org.nia.niamod.eventbus.Cancelable;

public record CommandSentEvent(String command) implements Cancelable {
}
