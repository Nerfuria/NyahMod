package org.nia.niamod.models.records;

import lombok.Getter;

@Getter
public enum State {
    FAVOURITE("§c"),
    NORMAL(""),
    AVOID("§8");

    private final String code;

    State(String code) {
        this.code = code;
    }
}
