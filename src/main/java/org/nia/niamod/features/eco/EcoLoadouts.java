package org.nia.niamod.features.eco;

import org.nia.niamod.models.territory.TerritoryLoadout;
import org.nia.niamod.models.territory.TerritoryUpgrade;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// testing bla bla
public final class EcoLoadouts {
    private static final List<TerritoryLoadout> DEFAULTS = List.of(
            loadout("MAX", Map.of(
                    TerritoryUpgrade.DAMAGE, 11,
                    TerritoryUpgrade.ATTACK, 11,
                    TerritoryUpgrade.HEALTH, 11,
                    TerritoryUpgrade.DEFENSE, 11,
                    TerritoryUpgrade.TOWER_AURA, 3,
                    TerritoryUpgrade.TOWER_VOLLEY, 3,
                    TerritoryUpgrade.STRONGER_MINIONS, 4,
                    TerritoryUpgrade.TOWER_MULTI_ATTACKS, 1
            ))
    );

    private EcoLoadouts() {
    }

    public static List<TerritoryLoadout> defaults() {
        return DEFAULTS;
    }

    private static TerritoryLoadout loadout(String name, Map<TerritoryUpgrade, Integer> levels) {
        EnumMap<TerritoryUpgrade, Integer> copy = new EnumMap<>(TerritoryUpgrade.class);
        copy.putAll(levels);
        return new TerritoryLoadout(name, copy);
    }
}
