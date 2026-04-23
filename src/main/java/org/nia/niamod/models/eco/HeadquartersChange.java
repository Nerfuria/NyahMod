package org.nia.niamod.models.eco;

public record HeadquartersChange(String territoryName) implements GameChange {
    public HeadquartersChange {
        territoryName = GameChanges.cleanTerritoryName(territoryName);
    }
}
