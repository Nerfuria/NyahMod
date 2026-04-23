package org.nia.niamod.models.eco;

public enum ResourceKind {
    EMERALDS("Emeralds"),
    CROPS("Crops"),
    WOOD("Wood"),
    ORE("Ore"),
    FISH("Fish"),
    ALL("All"),
    NONE("None");

    private final String label;

    ResourceKind(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
