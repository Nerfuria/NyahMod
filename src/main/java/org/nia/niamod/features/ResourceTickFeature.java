package org.nia.niamod.features;

import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.functions.Function;
import com.wynntils.core.consumers.functions.arguments.FunctionArguments;
import com.wynntils.models.territories.TerritoryInfo;
import com.wynntils.models.territories.type.GuildResource;
import com.wynntils.services.map.pois.TerritoryPoi;
import com.wynntils.utils.type.CappedValue;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.nia.niamod.NiamodClient;
import org.nia.niamod.models.misc.Feature;
import org.nia.niamod.models.misc.Safe;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResourceTickFeature extends Feature {

    private static final GuildResource[] RESOURCES = GuildResource.values();
    private static final int resTickOffset = 5;     // The map updates with a delay of 5 seconds for some reason
    public static boolean enabled = true;
    public Function<?> ResTickFunction = new ResTickFunction();
    private Integer lastMapTick = null;
    private String lastWorld = null;
    private Instant lastResTick = null;

    private static String get_world() {
        if (!Models.WorldState.onWorld()) {
            return null;
        }

        String currentWorldName = Models.WorldState.getCurrentWorldName();
        return currentWorldName.isEmpty() ? null : currentWorldName;
    }

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
        if (numbers.isEmpty()) return 0;

        Map<Integer, Integer> counts = new HashMap<>();
        for (int n : numbers) {
            counts.put(n, 1 + counts.getOrDefault(n, 0));
        }

        int mostCommon = numbers.getFirst();
        int maxCount = 0;

        for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > maxCount) {
                mostCommon = entry.getKey();
                maxCount = entry.getValue();
            }
        }

        return mostCommon;
    }

    @Override
    @Safe
    public void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!enabled) return;

            Instant currentTime = Instant.now();
            int currentMapTick = calcMapTick();

            if (lastMapTick == null || lastMapTick == currentMapTick) {
                lastMapTick = currentMapTick;
                return;
            }
            lastMapTick = currentMapTick;

            String currentWorld = get_world();
            if (currentWorld == null || !currentWorld.equals(lastWorld)) {
                lastWorld = currentWorld;
                return;
            }

            lastResTick = currentTime.minusSeconds(currentMapTick + resTickOffset);

            if (client.world != null) {
                long time = client.world.getTime();
                NiamodClient.LOGGER.info("Map tick changed to {} at world time {}", currentMapTick, time);
            }
        });
    }

    private int calcMapTick() {
        List<TerritoryPoi> TerritoryPois = Models.Territory.getTerritoryPoisFromAdvancement();
        List<Integer> map_ticks = new ArrayList<>();

        for (TerritoryPoi poi : TerritoryPois) {
            TerritoryInfo territoryInfo = poi.getTerritoryInfo();
            if (territoryInfo == null) continue;
            if (territoryInfo.isHeadquarters()) continue;   // Skip if HQ

            int emerald_gen = territoryInfo.getGeneration(GuildResource.EMERALDS);
            if (emerald_gen < 250000) continue;             // only check if 3-3 em prod and city

            boolean has_res_prods = false;
            for (GuildResource resource : RESOURCES) {
                if (!resource.isMaterialResource()) continue;
                if (territoryInfo.getGeneration(resource) >= 4800) {
                    has_res_prods = true;
                    break;
                }
            }
            if (has_res_prods) continue;                 // No res prods

            CappedValue emerald_storage = territoryInfo.getStorage(GuildResource.EMERALDS);
            if (emerald_storage == null || emerald_storage.max() < 6000)
                continue;     // min 1 emerald storage level

            CappedValue wood_storage = territoryInfo.getStorage(GuildResource.WOOD);
            if (wood_storage == null)
                continue;
            int res_storage_lvl = get_res_storage_lvl(wood_storage.max());
            if (res_storage_lvl < 1) continue;              // min 1 res storage level

            int res_storage_cost = get_res_storage_cost(res_storage_lvl);

            float emeralds_max = ((float) (emerald_gen - res_storage_cost)) / 60f;

            map_ticks.add(Math.round((emerald_storage.current() / emeralds_max) * 60));
        }

        return mode(map_ticks);
    }

    public int getTimeUntilResTick() {
        if (lastResTick == null) return -1;

        int secondsSinceResTick = (int) java.time.Duration.between(lastResTick, java.time.Instant.now()).getSeconds();

        return 60 - (secondsSinceResTick % 60);
    }

    public class ResTickFunction extends Function<Integer> {
        @Override
        public Integer getValue(FunctionArguments arguments) {
            return getTimeUntilResTick();
        }
    }
}


