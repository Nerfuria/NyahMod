package org.nia.niamod.models.territory;

public enum TerritoryRoute {
    FASTEST("Fastest"),
    CHEAPEST("Cheapest");

    private final String label;

    TerritoryRoute(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public TerritoryRoute toggled() {
        return this == FASTEST ? CHEAPEST : FASTEST;
    }
}
