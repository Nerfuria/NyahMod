package net.d0cr.niamod.features;

import net.d0cr.niamod.websocket.WebsocketHandler;
import net.minecraft.client.MinecraftClient;

import java.net.URI;

public class WebsocketFeature {

    public static boolean enabled = true;

    private WebsocketHandler websocketHandler;

    public void init(String serverId) {
        if (!enabled) return;
        String username = MinecraftClient.getInstance().getSession().getUsername();
        this.websocketHandler = new WebsocketHandler(URI.create("wss://localhost:6767"), username, serverId, this::handleMessage);
    }

    public void close() {
        this.websocketHandler.close();
    }

    public void handleMessage(String message) {

    }

}

