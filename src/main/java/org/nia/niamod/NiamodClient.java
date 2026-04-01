package org.nia.niamod;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.managers.Features;
import org.nia.niamod.managers.KeybindManager;
import org.nia.niamod.managers.Scheduler;
import org.slf4j.Logger;

public class NiamodClient implements ClientModInitializer {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static MinecraftClient mc;


    @Override
    public void onInitializeClient() {
        mc = MinecraftClient.getInstance();
        KeybindManager.init();
        NyahConfig.init();
        Features.init();
        Scheduler.init();
    }
}
