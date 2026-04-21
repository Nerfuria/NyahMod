package org.nia.niamod.models.gui.territory;

import java.util.Collection;

public record WorldBounds(double minX, double minZ, double maxX, double maxZ, boolean valid) {
    public static final WorldBounds EMPTY = new WorldBounds(0, 0, 0, 0, false);

    public static WorldBounds fromTerritories(Collection<TerritoryNode> territories) {
        if (territories.isEmpty()) {
            return EMPTY;
        }

        double minX = Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;
        for (TerritoryNode territory : territories) {
            minX = Math.min(minX, territory.centerX());
            minZ = Math.min(minZ, territory.centerZ());
            maxX = Math.max(maxX, territory.centerX());
            maxZ = Math.max(maxZ, territory.centerZ());
        }
        return new WorldBounds(minX, minZ, maxX, maxZ, true);
    }

    public double width() {
        return maxX - minX;
    }

    public double height() {
        return maxZ - minZ;
    }
}
