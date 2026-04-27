package org.nia.niamod.managers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.nia.niamod.models.records.TerritoryBase;
import org.nia.niamod.util.FileUtils;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class TerritoryBaseLoader {

    private static final String TERRITORY_FILE = "territory.json";
    private static final Gson GSON = new Gson();
    private static final Map<String, TerritoryBase> territories = new HashMap<>();

    public static void load() {
        Type type = new TypeToken<Map<String, JsonObject>>() {}.getType();
            Map<String, JsonObject> root = GSON.fromJson(FileUtils.readFile(TERRITORY_FILE), type);
            territories.clear();
            if (root == null)
                return;

            for (var entry : root.entrySet()) {
                if (entry.getKey().equals("_meta"))
                    continue;
                territories.put(entry.getKey(), TerritoryBase.fromJson(entry.getValue()));
            }
    }

    public static Map<String, TerritoryBase> getTerritories() {
        return java.util.Collections.unmodifiableMap(territories);
    }

    public static TerritoryBase getTerritory(String name) {
        return territories.get(name);
    }
}
