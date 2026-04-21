package org.nia.niamod.models.gui.territory;

public record TerritoryResourceStore(long current, long max) {
    public static final TerritoryResourceStore EMPTY = new TerritoryResourceStore(0L, 0L);

    public TerritoryResourceStore {
        current = Math.max(0L, current);
        max = Math.max(0L, max);
        if (max > 0L && current > max) {
            current = max;
        }
    }
}
