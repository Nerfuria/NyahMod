package org.nia.niamod.models.ignore;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IgnorePlayerMode {
    FAVOURITE(0),
    NONE(3),
    AVOID(4);

    private final int sortOrder;
}
