package org.nia.niamod.util;

import com.wynntils.models.territories.TerritoryInfo;
import com.wynntils.models.territories.type.GuildResource;
import com.wynntils.utils.type.CappedValue;
import lombok.experimental.UtilityClass;

@UtilityClass
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
}
