package org.nia.niamod.models.ignore;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class IgnorePlayerEntry {
    private final String playerName;
    private final IgnorePlayerMode mode;
    private final boolean ignored;
    private final boolean modeEditable;
}
