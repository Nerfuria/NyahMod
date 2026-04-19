package org.nia.niamod.models.ignore;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IgnoreAction {
    IGNORE(true, "add"),
    UNIGNORE(false, "remove");

    private final boolean ignoredState;
    private final String commandAction;

    public IgnoreAction opposite() {
        return this == IGNORE ? UNIGNORE : IGNORE;
    }
}
