package org.nia.niamod.features;

import com.wynntils.core.components.Models;
import com.wynntils.models.territories.TerritoryInfo;
import com.wynntils.models.territories.type.GuildResource;
import com.wynntils.services.map.pois.TerritoryPoi;
import com.wynntils.utils.type.CappedValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResourceTickFeature {

    public static boolean enabled = true;

    private static int get_res_storage_lvl(int res_storage_max) {
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

    private static int get_res_storage_cost(int res_storage_lvl) {
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

    private static int mode(List<Integer> numbers) {
        Map<Integer, Integer> counts = new HashMap<>();
        for (int n : numbers) {
            counts.put(n, 1 + counts.getOrDefault(n, 0));
        }

        int mostCommon = numbers.get(0);
        int maxCount = 0;
        for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > maxCount) {
                mostCommon = entry.getKey();
                maxCount = entry.getValue();
            }
        }

        return mostCommon;
    }

    public void init() {
    }

    public int calcMapTick() {
        List<TerritoryPoi> TerritoryPois = Models.Territory.getTerritoryPoisFromAdvancement();
        List<Integer> map_ticks = new ArrayList<>();

        for (TerritoryPoi poi : TerritoryPois) {
            TerritoryInfo territoryInfo = poi.getTerritoryInfo();
            if (territoryInfo == null) continue;
            if (territoryInfo.isHeadquarters()) continue;   // Skip if HQ

            int emerald_gen = territoryInfo.getGeneration(GuildResource.EMERALDS);
            if (emerald_gen < 250000) continue;             // 3-3 em prod and city

            boolean has_res_prods = false;
            for (GuildResource resource : GuildResource.values()) {
                if (!resource.isMaterialResource()) continue;
                int resource_gen = territoryInfo.getGeneration(resource);
                has_res_prods = has_res_prods || resource_gen >= 4800;
            }
            if (has_res_prods) continue;                    // No res prods

            CappedValue emerald_storage = territoryInfo.getStorage(GuildResource.EMERALDS);
            if (emerald_storage.max() < 6000) continue;     // min 1 emerald storage level

            int res_storage_lvl = get_res_storage_lvl(territoryInfo.getStorage(GuildResource.WOOD).max());
            if (res_storage_lvl < 1) continue;              // min 1 res storage level

            int res_storage_cost = get_res_storage_cost(res_storage_lvl);

            float emeralds_max = ((float) (emerald_gen - res_storage_cost)) / 60;

            map_ticks.add(Math.round((emerald_storage.current() / emeralds_max) * 60));
        }

        return mode(map_ticks);
    }
}


