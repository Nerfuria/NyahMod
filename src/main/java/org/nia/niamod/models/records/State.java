package org.nia.niamod.models.records;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum State {
    FAVOURITE("§c"),
    NORMAL(""),
    AVOID("§8");

    private final String code;
}
