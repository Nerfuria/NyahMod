package org.nia.niamod.models.eco;

public record BordersChange(String territoryName, boolean open) implements GameChange {
    public BordersChange {
        territoryName = GameChanges.cleanTerritoryName(territoryName);
    }
}
