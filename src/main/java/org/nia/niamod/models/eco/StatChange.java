package org.nia.niamod.models.eco;

public record StatChange(String territoryName, TerritoryUpgrade upgrade, int level) implements GameChange {
    public StatChange {
        territoryName = GameChanges.cleanTerritoryName(territoryName);
    }
}
