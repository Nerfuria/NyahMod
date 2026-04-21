package org.nia.niamod.models.gui.territory;

public record DamageRange(double min, double max) {
    public static final DamageRange EMPTY = new DamageRange(0.0, 0.0);

    public double average() {
        return (min + max) / 2.0;
    }
}
