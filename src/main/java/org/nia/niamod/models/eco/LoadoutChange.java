package org.nia.niamod.models.eco;

import java.util.Collection;
import java.util.List;

public record LoadoutChange(TerritoryLoadout loadout, List<String> territoryNames) implements GameChange {
    public LoadoutChange(TerritoryLoadout loadout, Collection<String> territoryNames) {
        this(loadout, territoryNames == null
                ? List.of()
                : territoryNames.stream()
                .map(GameChanges::cleanTerritoryName)
                .filter(territoryName -> !territoryName.isEmpty())
                .toList());
    }

    public LoadoutChange {
        territoryNames = territoryNames == null ? List.of() : List.copyOf(territoryNames);
    }
}
