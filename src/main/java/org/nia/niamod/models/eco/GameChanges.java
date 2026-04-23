package org.nia.niamod.models.eco;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class GameChanges {
    public static String cleanTerritoryName(String value) {
        return value == null ? "" : value.trim();
    }
}
