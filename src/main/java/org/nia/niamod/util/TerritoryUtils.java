package org.nia.niamod.util;

import com.wynntils.models.territories.TerritoryInfo;
import com.wynntils.models.territories.type.GuildResource;
import com.wynntils.models.territories.type.TerritoryUpgrade;
import com.wynntils.utils.type.CappedValue;
import com.wynntils.utils.type.Pair;
import org.nia.niamod.managers.FeatureManager;

import java.util.HashMap;
import java.util.Map;

public class TerritoryUtils {
    public static final GuildResource[] RESOURCES = GuildResource.values();

    public static int resStorageCostToLevel(int res_storage_max) {
        return switch (res_storage_max) {
            case 300 -> 0;
            case 600 -> 1;
            case 1200 -> 2;
            case 2400 -> 3;
            case 4500 -> 4;
            case 10200 -> 5;
            case 24000 -> 6;
            default -> -1;
        };
    }

    public static int resStorageLevelToCost(int res_storage_lvl) {
        return switch (res_storage_lvl) {
            case 1 -> 400;
            case 2 -> 800;
            case 3 -> 2000;
            case 4 -> 5000;
            case 5 -> 16000;
            case 6 -> 48000;
            default -> 0;
        };
    }

    public static int getResStorageLevel(TerritoryInfo territoryInfo) {
        for (GuildResource resource : GuildResource.values()) {
            if (!resource.isMaterialResource())
                continue;

            CappedValue storage = territoryInfo.getStorage(resource);
            if (storage != null)
                return TerritoryUtils.resStorageCostToLevel(storage.max());
        }
        return 0;
    }

    /**
     * Get the resource cost of a territory.
     **/
    public static int getResCost(TerritoryInfo territoryInfo, GuildResource resource) {
        int mapTick = FeatureManager.getResTickFeature().getMapTick();
        if (mapTick <= 0)
            return 0;

        CappedValue storage = territoryInfo.getStorage(resource);
        if (storage == null)
            return 0;

        int prod = territoryInfo.getGeneration(resource);

        return (storage.current() * 60 * 60 / mapTick) - prod;
    }

    private static Pair<Integer, Integer> findUpgradeLevels(TerritoryUpgrade upgrade, TerritoryUpgrade bonus, int targetCost, boolean forceBonus) {
        Pair<Integer, Integer> best = new Pair<>(0, 0);
        long bestCost = 0;

        for (int bonusLevel = forceBonus ? 1 : 0; bonusLevel < bonus.getLevels().length; bonusLevel++) {
            long bonusCost = bonus.getLevels()[bonusLevel].cost();
            for (int upgradeLevel = 0; upgradeLevel < upgrade.getLevels().length; upgradeLevel++) {
                long upgradeCost = upgrade.getLevels()[upgradeLevel].cost();

                long totalCost = upgradeCost + bonusCost;
                if (totalCost <= targetCost + 50 && totalCost > bestCost) {
                    best = new Pair<>(upgradeLevel, bonusLevel);
                    bestCost = upgradeCost + bonusCost;
                } else if (totalCost > targetCost + 50)
                    break;
            }
        }
        return best;
    }

    public static Map<TerritoryUpgrade, Integer> estimateUpgrades(TerritoryInfo territoryInfo) {
        Map<TerritoryUpgrade, Integer> result = new HashMap<>();

        if (territoryInfo.isHeadquarters())
            return result;

        boolean hasAuraVolley = territoryInfo.getDefences().getLevel() >= 3;

        GuildResource[] resources = {GuildResource.ORE, GuildResource.CROPS, GuildResource.WOOD, GuildResource.FISH};
        TerritoryUpgrade[] upgrades = {TerritoryUpgrade.DAMAGE, TerritoryUpgrade.ATTACK, TerritoryUpgrade.HEALTH, TerritoryUpgrade.DEFENCE};
        TerritoryUpgrade[] bonuses = {TerritoryUpgrade.TOWER_VOLLEY, TerritoryUpgrade.TOWER_AURA, TerritoryUpgrade.STRONGER_MINIONS, TerritoryUpgrade.TOWER_MULTI_ATTACKS};

        int[] otherCosts = {0, 0, 0, 0};
        // TODO Emerald storage + prods

        for (int i = 0; i < resources.length; i++) {
            int cost = getResCost(territoryInfo, resources[i]);
            cost -= otherCosts[i];

            boolean forceAuraVolley = hasAuraVolley && (i == 0 || i == 1);

            Pair<Integer, Integer> levels = findUpgradeLevels(upgrades[i], bonuses[i], cost, forceAuraVolley);

            result.put(upgrades[i], levels.a());
            result.put(bonuses[i], levels.b());
        }

        return result;
    }
}
