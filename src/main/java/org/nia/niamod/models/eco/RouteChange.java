package org.nia.niamod.models.eco;

public record RouteChange(String territoryName, TerritoryRoute route) implements GameChange {
    public RouteChange {
        territoryName = GameChanges.cleanTerritoryName(territoryName);
    }
}
