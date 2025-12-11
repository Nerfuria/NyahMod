package org.nia.niamod;

import com.mojang.logging.LogUtils;
import org.nia.niamod.features.Features;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class NiamodClient implements ClientModInitializer {
    public static final Logger LOGGER = LogUtils.getLogger();


    @Override
    public void onInitializeClient() {
        Features.init();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                literal("maptick") // command name
                    .executes(context -> {
                        int tick = Features.getResTickFeature().calcMapTick();
                        MinecraftClient.getInstance().player.sendMessage(Text.literal(String.valueOf(tick)), false);
                        return 1;
                    })
            );
        });
    }
}
