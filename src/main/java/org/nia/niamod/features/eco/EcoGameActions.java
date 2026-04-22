package org.nia.niamod.features.eco;

import org.nia.niamod.models.territory.TerritoryLoadout;
import org.nia.niamod.models.territory.TerritoryRoute;
import org.nia.niamod.models.territory.TerritoryUpgrade;

import java.util.Collection;

public class EcoGameActions {
    public void pushStat(String territoryName, TerritoryUpgrade stat, int level) {
        // TODO: Push this territory stat level through the game command/application bridge.
    }

    public void pushHeadquarters(String territoryName) {
        // TODO: Push the HQ territory through the game command/application bridge.
    }

    public void pushTax(String territoryName, int percent) {
        // TODO: Push this territory tax through the game command/application bridge.
    }

    public void pushBorders(String territoryName, boolean open) {
        // TODO: Push this territory border state through the game command/application bridge.
    }

    public void pushRoute(String territoryName, TerritoryRoute route) {
        // TODO: Push this territory route through the game command/application bridge.
    }

    public void applyLoadout(TerritoryLoadout loadout, Collection<String> territoryNames) {
        // TODO: Apply the named loadout to these territories through the game command/application bridge.
    }
}
