package org.nia.niamod.models.ignore;

public enum IgnoreAction {
    IGNORE(true, "add"),
    UNIGNORE(false, "remove");

    private final boolean ignoredState;
    private final String commandAction;

    IgnoreAction(boolean ignoredState, String commandAction) {
        this.ignoredState = ignoredState;
        this.commandAction = commandAction;
    }

    public boolean ignoredState() {
        return ignoredState;
    }

    public String commandAction() {
        return commandAction;
    }

    public IgnoreAction opposite() {
        return this == IGNORE ? UNIGNORE : IGNORE;
    }
}
