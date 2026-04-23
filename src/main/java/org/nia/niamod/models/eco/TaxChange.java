package org.nia.niamod.models.eco;

public record TaxChange(String territoryName, int percent) implements GameChange {
    public TaxChange {
        territoryName = GameChanges.cleanTerritoryName(territoryName);
    }
}
