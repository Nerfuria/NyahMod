package org.nia.niamod.models.records;

public enum State {
    FAVOURITE("§c"),
    NORMAL(""),
    AVOID("§8");

    public final String code;

    State(String code) {
        this.code = code;
    }
}