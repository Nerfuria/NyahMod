package org.nia.niamod.features;

import net.minecraft.client.MinecraftClient;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.websocket.WebsocketHandler;

import java.net.URI;

public class WebsocketFeature {


    private WebsocketHandler websocketHandler;

    public void init(String serverId) {
        if (!NyahConfig.nyahConfigData.wsEnabled) return;
        String username = MinecraftClient.getInstance().getSession().getUsername();
        this.websocketHandler = new WebsocketHandler(URI.create(NyahConfig.nyahConfigData.wsURL), username, serverId, this::handleMessage);
    }

    public void close() {
        if (websocketHandler == null) return;
        this.websocketHandler.close();
    }

    public void handleMessage(String message) {
        // TODO: Implement messages with WSw
    }

}

