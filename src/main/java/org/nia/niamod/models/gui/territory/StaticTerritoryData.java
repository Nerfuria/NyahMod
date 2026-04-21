package org.nia.niamod.models.gui.territory;

import java.util.List;

public record StaticTerritoryData(Resources resources, List<String> connections) {
    public StaticTerritoryData {
        if (resources == null) {
            resources = Resources.EMPTY;
        }
        if (connections == null) {
            connections = List.of();
        }
    }
}
