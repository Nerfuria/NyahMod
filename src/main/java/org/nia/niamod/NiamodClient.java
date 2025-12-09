package org.nia.niamod;

import com.mojang.logging.LogUtils;
import org.nia.niamod.features.Features;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;

public class NiamodClient implements ClientModInitializer {
    public static final Logger LOGGER = LogUtils.getLogger();


    @Override
    public void onInitializeClient() {
        Features.init();
    }
}
