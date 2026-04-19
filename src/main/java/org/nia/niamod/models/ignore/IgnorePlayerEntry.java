package org.nia.niamod.models.ignore;

public record IgnorePlayerEntry(String playerName, IgnorePlayerMode mode, boolean ignored, boolean modeEditable) {
}
