package org.nia.niamod.managers;

import org.nia.niamod.managers.war.TerritoryBaseLoader;

public class ModDataManager {
    public static void loadAll() {
        TerritoryBaseLoader.load();
    }
}