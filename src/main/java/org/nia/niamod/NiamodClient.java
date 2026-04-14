package org.nia.niamod;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.managers.FeatureManager;
import org.nia.niamod.managers.KeybindManager;
import org.nia.niamod.managers.OverlayManager;
import org.nia.niamod.managers.Scheduler;
import org.slf4j.Logger;

public class NiamodClient implements ClientModInitializer {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static Minecraft mc;

    @Override
    public void onInitializeClient() {
        mc = Minecraft.getInstance();
        KeybindManager.init();
        NyahConfig.init();
        Scheduler.init();
        OverlayManager.init();
        FeatureManager.init();
    }
}
