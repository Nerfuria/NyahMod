package org.nia.niamod.features;

import net.minecraft.client.MinecraftClient;
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
import org.nia.niamod.util.MathUtils;
import org.nia.niamod.util.TerritoryUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ResourceTickFeature extends Feature {
    private static final int resTickOffset = 5;     // The map updates with a delay of 5 seconds for some reason
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

    @Override
    @Safe
    public void init() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    @Safe
    private void onClientTick(MinecraftClient client) {
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
            for (GuildResource resource : TerritoryUtils.RESOURCES) {
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

            int res_storage_lvl = TerritoryUtils.getResStorageLevel(territoryInfo);
            if (res_storage_lvl < 1) continue;              // min 1 res storage level
            int res_storage_cost = TerritoryUtils.resStorageLevelToCost(res_storage_lvl);

            float emeralds_max = ((float) (emerald_gen - res_storage_cost)) / 60f;

            map_ticks.add(Math.round((emerald_storage.current() / emeralds_max) * 60));
        }

        return MathUtils.mode(map_ticks);
    }

    @Safe
    public int getTimeUntilResTick() {
        if (lastResTick == null) return -1;

        int secondsSinceResTick = (int) java.time.Duration.between(lastResTick, java.time.Instant.now()).getSeconds();

        return 60 - (secondsSinceResTick % 60);
    }

    @Safe
    public int getMapTick() {
        if (lastMapTick == null) return -1;
        return lastMapTick;
    }

    public class ResTickFunction extends Function<Integer> {
        @Override
        public Integer getValue(FunctionArguments arguments) {
            return getTimeUntilResTick();
        }
    }
}


