package org.nia.niamod.models.ignore;

public enum IgnorePlayerMode {
    FAVOURITE(0),
    NONE(3),
    AVOID(4);

    private final int sortOrder;

    IgnorePlayerMode(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public int sortOrder() {
        return sortOrder;
    }
}
