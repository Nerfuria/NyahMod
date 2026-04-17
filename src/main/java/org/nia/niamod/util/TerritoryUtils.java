package org.nia.niamod.util;

import com.wynntils.core.components.Models;
import com.wynntils.models.territories.TerritoryInfo;
import com.wynntils.models.territories.type.GuildResource;
import com.wynntils.models.territories.type.GuildResourceValues;
import com.wynntils.models.territories.type.TerritoryUpgrade;
import com.wynntils.utils.type.CappedValue;
import com.wynntils.utils.type.Pair;
import org.nia.niamod.managers.FeatureManager;
import org.nia.niamod.managers.war.TerritoryBaseLoader;

import java.util.*;

public class TerritoryUtils {
    public static final GuildResource[] RESOURCES = GuildResource.values();
    private final static Map<Integer, List<EmeraldProdUpgrade>> EMERALD_MODIFIER_OPTIONS = getEmeraldModifierOptions();

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

    public static int resStorageLevelToCost(int resStorageLevel) {
        return Math.toIntExact(TerritoryUpgrade.RESOURCE_STORAGE.getLevels()[resStorageLevel].cost());
    }

    public static int getResStorageLevel(TerritoryInfo territoryInfo) {
        int hqModifier = territoryInfo.isHeadquarters() ? 5 : 1;
        for (GuildResource resource : GuildResource.values()) {
            if (!resource.isMaterialResource())
                continue;

            CappedValue storage = territoryInfo.getStorage(resource);
            if (storage != null)
                return TerritoryUtils.resStorageCostToLevel(storage.max() / hqModifier);
        }
        return 0;
    }

    public static int emeraldStorageLevelToCost(int emeraldStorageLevel) {
        return Math.toIntExact(TerritoryUpgrade.EMERALD_STORAGE.getLevels()[emeraldStorageLevel].cost());
    }

    public static int getEmeraldStorageLevel(TerritoryInfo territoryInfo) {
        int base = territoryInfo.isHeadquarters() ? 5000 : 3000;
        CappedValue storage = territoryInfo.getStorage(GuildResource.EMERALDS);
        if (storage == null)
            return 0;

        int bonus = (storage.max() * 100) / base - 100;
        TerritoryUpgrade.Level[] levels = TerritoryUpgrade.EMERALD_STORAGE.getLevels();
        for (int i = 0; i < levels.length; i++) {
            if (levels[i].bonus() == bonus)
                return i;
        }
        return -1;
    }

    private static Map<Integer, List<EmeraldProdUpgrade>> getEmeraldModifierOptions() {
        Map<Integer, List<EmeraldProdUpgrade>> result = new HashMap<>();
        for (int effLvl = 0; effLvl < TerritoryUpgrade.EFFICIENT_EMERALDS.getLevels().length; effLvl++) {
            for (int rateLvl = 0; rateLvl < TerritoryUpgrade.EMERALD_RATE.getLevels().length; rateLvl++) {
                double eff = TerritoryUpgrade.EFFICIENT_EMERALDS.getLevels()[effLvl].bonus();
                double rate = TerritoryUpgrade.EMERALD_RATE.getLevels()[rateLvl].bonus();
                int modifier = (int) ((100 + eff) * (4.0 / rate));
                if (!result.containsKey(modifier))
                    result.put(modifier, new ArrayList<>());
                result.get(modifier).add(new EmeraldProdUpgrade(
                        effLvl,
                        rateLvl,
                        Math.toIntExact(TerritoryUpgrade.EFFICIENT_EMERALDS.getLevels()[effLvl].cost()),
                        Math.toIntExact(TerritoryUpgrade.EMERALD_RATE.getLevels()[rateLvl].cost())
                ));
            }
        }
        return Map.copyOf(result);
    }

    private static EmeraldProdUpgrade getMostLikelyEmProdUpgrade(String territoryName, TerritoryInfo territoryInfo) {
        double treasuryBonus = getTreasuryBonus(territoryName);
        int modifier = getProductionModifier(territoryName, territoryInfo, GuildResource.EMERALDS, treasuryBonus);
        List<EmeraldProdUpgrade> options = EMERALD_MODIFIER_OPTIONS.get(modifier);
        if (options == null)
            return null;

        int baseCrop = getResCost(territoryInfo, GuildResource.CROPS);
        int baseOre = getResCost(territoryInfo, GuildResource.ORE);

        int bestOption = -1;
        float bestDiff = Integer.MAX_VALUE;

        for (int i = 0; i < options.size(); i++) {
            int restCrop = baseCrop - options.get(i).cropCost;
            int restOre = baseOre - options.get(i).oreCost;

            if (restCrop <= 0 || restOre <= 0)
                continue;

            int diff = Math.abs(restCrop - restOre);
            if (diff < bestDiff) {
                bestOption = i;
                bestDiff = diff;
            }
        }
        if (bestOption == -1)
            return null;
        return options.get(bestOption);
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

        return (storage.current() * 60 * 60 - (prod * mapTick)) / (60 - mapTick);
    }

    private static Pair<Integer, Integer> findUpgradeLevels(TerritoryUpgrade upgrade, TerritoryUpgrade bonus, int targetCost, boolean forceBonus) {
        Pair<Integer, Integer> best = new Pair<>(0, 0);
        long bestCost = 0;

        for (int bonusLevel = forceBonus ? 1 : 0; bonusLevel < bonus.getLevels().length; bonusLevel++) {
            long bonusCost = bonus.getLevels()[bonusLevel].cost();
            for (int upgradeLevel = 0; upgradeLevel < upgrade.getLevels().length; upgradeLevel++) {
                long upgradeCost = upgrade.getLevels()[upgradeLevel].cost();

                long totalCost = upgradeCost + bonusCost;
                if (totalCost <= targetCost + 99 && totalCost > bestCost) {
                    best = new Pair<>(upgradeLevel, bonusLevel);
                    bestCost = upgradeCost + bonusCost;
                } else if (totalCost > targetCost + 50)
                    break;
            }
        }
        return best;
    }

    public static int getHQDistance(String territoryName) {
        TerritoryInfo startTerr = Models.Territory.getTerritoryPoiFromAdvancement(territoryName).getTerritoryInfo();
        String guild = startTerr.getGuildName();

        if (startTerr.isHeadquarters())
            return 0;

        ArrayDeque<Pair<TerritoryInfo, Integer>> queue = new ArrayDeque<>();
        Set<String> checked = new HashSet<>();
        queue.add(new Pair<>(startTerr, 0));
        checked.add(territoryName);

        while (!queue.isEmpty()) {
            var next = queue.poll();
            TerritoryInfo terr = next.a();
            int dist = next.b();

            List<String> conns = terr.getTradingRoutes();
            for (String connName : conns) {
                if (checked.contains(connName))
                    continue;
                checked.add(connName);

                TerritoryInfo conn = Models.Territory.getTerritoryPoiFromAdvancement(connName).getTerritoryInfo();
                if (!guild.equals(conn.getGuildName()))
                    continue;
                if (conn.isHeadquarters())
                    return dist + 1;

                queue.add(new Pair<>(conn, dist + 1));
            }
        }
        return -1;
    }

    public static double getTreasuryBonus(String territoryName) {
        TerritoryInfo territory = Models.Territory.getTerritoryPoiFromAdvancement(territoryName).getTerritoryInfo();
        GuildResourceValues level = territory.getTreasury();

        if (level.getLevel() <= 1)
            return 0.0;

        double tresValue = switch (level) {
            case NONE, VERY_LOW -> 0.0;
            case LOW -> 0.1;
            case MEDIUM -> 0.2;
            case HIGH -> 0.25;
            case VERY_HIGH -> 0.3;
        };

        int distance = getHQDistance(territoryName);
        double distanceModifier = switch (distance) {
            case 3 -> 0.85;
            case 4 -> 0.7;
            case 5 -> 0.55;
            default -> distance >= 6 ? 0.4 : 1.0;
        };

        return tresValue * distanceModifier;
    }

    /**
     * Get the resource production modifier of a territory in % for a specified resource.
     */
    public static int getProductionModifier(String territoryName, TerritoryInfo territoryInfo, GuildResource resource, double treasuryBonus) {
        int baseProd = TerritoryBaseLoader.getTerritory(territoryName).getProduction(resource);
        int currProd = territoryInfo.getGeneration(resource);
        if (baseProd == 0)
            return 0;
        return (int) ((currProd * 100) / (1.0 + treasuryBonus) / baseProd);
    }

    public static Map<TerritoryUpgrade, Integer> estimateUpgrades(String territoryName) {
        TerritoryInfo territoryInfo = Models.Territory.getTerritoryPoiFromAdvancement(territoryName).getTerritoryInfo();

        Map<TerritoryUpgrade, Integer> result = new HashMap<>();

        if (territoryInfo.isHeadquarters())
            return result;

        boolean hasAuraVolley = territoryInfo.getDefences().getLevel() >= 3;

        GuildResource[] resources = {GuildResource.ORE, GuildResource.CROPS, GuildResource.WOOD, GuildResource.FISH};
        TerritoryUpgrade[] upgrades = {TerritoryUpgrade.DAMAGE, TerritoryUpgrade.ATTACK, TerritoryUpgrade.HEALTH, TerritoryUpgrade.DEFENCE};
        TerritoryUpgrade[] bonuses = {TerritoryUpgrade.TOWER_VOLLEY, TerritoryUpgrade.TOWER_AURA, TerritoryUpgrade.STRONGER_MINIONS, TerritoryUpgrade.TOWER_MULTI_ATTACKS};

        int[] otherCosts = {0, 0, 0, 0};
        // Emerald storage
        int emStorageLvl = getEmeraldStorageLevel(territoryInfo);
        if (emStorageLvl > 0)
            otherCosts[2] = emeraldStorageLevelToCost(emStorageLvl);
        // Emerald prod
        EmeraldProdUpgrade emeraldProdUpgrade = getMostLikelyEmProdUpgrade(territoryName, territoryInfo);
        if (emeraldProdUpgrade != null) {
            otherCosts[0] = emeraldProdUpgrade.oreCost;
            otherCosts[1] = emeraldProdUpgrade.cropCost;
        }

        for (int i = 0; i < resources.length; i++) {
            int cost = getResCost(territoryInfo, resources[i]);
            cost -= otherCosts[i];

            boolean forceBonus = hasAuraVolley && (i == 0 || i == 1);

            Pair<Integer, Integer> levels = findUpgradeLevels(upgrades[i], bonuses[i], cost, forceBonus);

            result.put(upgrades[i], levels.a());
            result.put(bonuses[i], levels.b());
        }

        return result;
    }

    private record EmeraldProdUpgrade(int efficientEmeralds, int emeraldRate, int oreCost, int cropCost) {}
}
