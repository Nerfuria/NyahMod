package org.nia.niamod.features.eco;

import org.nia.niamod.models.eco.*;

import java.util.Collection;
import java.util.List;

public class GameActions {
    public void apply(GameChange change) {
        switch (change) {
            case StatChange(String territoryName, TerritoryUpgrade upgrade, int level) -> {
                pushStat(territoryName, upgrade, level);
            }
            case HeadquartersChange(String territoryName) -> {
                pushHeadquarters(territoryName);
            }
            case TaxChange(String territoryName, int percent) -> {
                pushTax(territoryName, percent);
            }
            case BordersChange(String territoryName, boolean open) -> {
                pushBorders(territoryName, open);
            }
            case RouteChange(String territoryName, TerritoryRoute route) -> {
                pushRoute(territoryName, route);
            }
            case LoadoutChange(TerritoryLoadout loadout, List<String> territoryNames) -> {
                applyLoadout(loadout, territoryNames);
            }
            case GlobalTaxChange(int percent) -> {
                pushGlobalTax(percent);
            }
            case GlobalBordersChange(boolean open) -> {
                pushGlobalBorders(open);
            }
            case GlobalRouteChange(TerritoryRoute route) -> pushGlobalRoute(route);
            case null, default -> {
            }
        }

    }

    public List<TerritoryLoadout> loadLoadouts() {
        // TODO: Replace this fallback with game-loaded loadouts when the bridge exposes them.
        return Loadouts.defaults();
    }

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

    public void pushGlobalTax(int percent) {
        // TODO: Push this global tax change through the game command/application bridge.
    }

    public void pushGlobalBorders(boolean open) {
        // TODO: Push this global borders change through the game command/application bridge.
    }

    public void pushGlobalRoute(TerritoryRoute route) {
        // TODO: Push this global route change through the game command/application bridge.
    }

    public void applyLoadout(TerritoryLoadout loadout, Collection<String> territoryNames) {
        // TODO: Apply the named loadout to these territories through the game command/application bridge.
    }
}
