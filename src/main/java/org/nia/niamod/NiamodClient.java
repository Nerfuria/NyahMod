package org.nia.niamod;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ClientModInitializer;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.managers.FeatureManager;
import org.nia.niamod.managers.KeybindManager;
import org.nia.niamod.managers.OverlayManager;
import org.nia.niamod.managers.Scheduler;
import org.nia.niamod.managers.TerritoryBaseLoader;
import org.slf4j.Logger;

public class NiamodClient implements ClientModInitializer {
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitializeClient() {
        KeybindManager.init();
        NyahConfig.init();
        Scheduler.init();
        OverlayManager.init();
        TerritoryBaseLoader.load();
        FeatureManager.init();
    }
}
