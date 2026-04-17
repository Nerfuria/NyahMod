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

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TerritoryUtils {
    public static final GuildResource[] RESOURCES = GuildResource.values();

    public static int resStorageCapToLevel(int res_storage_max) {
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
                return TerritoryUtils.resStorageCapToLevel(storage.max() / hqModifier);
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

    /**
     * Get the distance of a territory from its HQ using BFS.
     */
    public static int getHQDistance(String territoryName) {
        TerritoryInfo startTerr = Models.Territory.getTerritoryPoiFromAdvancement(territoryName).getTerritoryInfo();
        String guild = startTerr.getGuildName();

        if (guild == null)
            return Integer.MAX_VALUE;

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
        return Integer.MAX_VALUE;
    }

    /**
     * Get the treasury bonus of a territory (dependent on treasury level and distance to HQ).
     */
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
            case 0, 1, 2 -> 1.0;
            case 3 -> 0.85;
            case 4 -> 0.7;
            case 5 -> 0.55;
            default -> 0.4;
        };

        return tresValue * distanceModifier;
    }

    /**
     * Get the resource production modifier of a territory for a specified resource (includes treasury bonus).
     */
    public static double getProductionModifier(String territoryName, GuildResource resource) {
        int baseProd = TerritoryBaseLoader.getTerritory(territoryName).getProduction(resource);
        TerritoryInfo territoryInfo = Models.Territory.getTerritoryPoiFromAdvancement(territoryName).getTerritoryInfo();
        int currProd = territoryInfo.getGeneration(resource);
        if (baseProd == 0)
            return 0;
        return ((double) currProd) / baseProd;
    }
}
