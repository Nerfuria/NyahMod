package org.nia.niamod.models.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.nia.niamod.eventbus.Cancelable;

@Getter
@Setter
@AllArgsConstructor
public class CommandSentEvent implements Cancelable {
    private String command;

    public String command() {
        return command;
    }
}
