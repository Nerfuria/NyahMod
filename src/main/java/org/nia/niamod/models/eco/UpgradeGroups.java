package org.nia.niamod.models.eco;

import java.util.ArrayList;
import java.util.List;

public final class UpgradeGroups {
    private static final List<TerritoryUpgrade> COMBAT = List.of(
            TerritoryUpgrade.DAMAGE,
            TerritoryUpgrade.ATTACK,
            TerritoryUpgrade.HEALTH,
            TerritoryUpgrade.DEFENSE,
            TerritoryUpgrade.TOWER_AURA,
            TerritoryUpgrade.STRONGER_MINIONS,
            TerritoryUpgrade.TOWER_VOLLEY,
            TerritoryUpgrade.TOWER_MULTI_ATTACKS
    );

    private static final List<TerritoryUpgrade> ECONOMY = List.of(
            TerritoryUpgrade.RESOURCE_STORAGE,
            TerritoryUpgrade.EMERALD_STORAGE,
            TerritoryUpgrade.RESOURCE_RATE,
            TerritoryUpgrade.EFFICIENT_RESOURCES,
            TerritoryUpgrade.EMERALD_RATE,
            TerritoryUpgrade.EFFICIENT_EMERALDS
    );

    private static final List<TerritoryUpgrade> STORAGE = List.of(
            TerritoryUpgrade.RESOURCE_STORAGE,
            TerritoryUpgrade.EMERALD_STORAGE
    );

    private static final List<TerritoryUpgrade> PRODUCTION = List.of(
            TerritoryUpgrade.RESOURCE_RATE,
            TerritoryUpgrade.EFFICIENT_RESOURCES,
            TerritoryUpgrade.EMERALD_RATE,
            TerritoryUpgrade.EFFICIENT_EMERALDS
    );

    private static final List<TerritoryUpgrade> UTILITY = List.of(
            TerritoryUpgrade.GATHERING_EXPERIENCE,
            TerritoryUpgrade.MOB_EXPERIENCE,
            TerritoryUpgrade.MOB_DAMAGE,
            TerritoryUpgrade.PVP_DAMAGE,
            TerritoryUpgrade.XP_SEEKING,
            TerritoryUpgrade.TOME_SEEKING,
            TerritoryUpgrade.EMERALD_SEEKING
    );

    private static final List<TerritoryUpgrade> QUICK_MENU = quickMenuUpgrades();

    private UpgradeGroups() {
    }

    public static List<TerritoryUpgrade> combat() {
        return COMBAT;
    }

    public static List<TerritoryUpgrade> economy() {
        return ECONOMY;
    }

    public static List<TerritoryUpgrade> storage() {
        return STORAGE;
    }

    public static List<TerritoryUpgrade> production() {
        return PRODUCTION;
    }

    public static List<TerritoryUpgrade> utility() {
        return UTILITY;
    }

    public static List<TerritoryUpgrade> quickMenu() {
        return QUICK_MENU;
    }

    private static List<TerritoryUpgrade> quickMenuUpgrades() {
        ArrayList<TerritoryUpgrade> upgrades = new ArrayList<>(COMBAT.size() + PRODUCTION.size());
        upgrades.addAll(COMBAT);
        upgrades.addAll(PRODUCTION);
        return List.copyOf(upgrades);
    }
}
