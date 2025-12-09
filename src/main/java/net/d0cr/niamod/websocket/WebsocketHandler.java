package net.d0cr.niamod.websocket;

import net.d0cr.niamod.client.NiamodClient;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class WebsocketHandler extends WebSocketClient {
    private final String playerName;
    private final String serverId;
    private final MessageHandler messageHandler;

    public WebsocketHandler(URI serverURI, String playerName, String serverId, MessageHandler messageHandler) {
        super(serverURI);

        this.playerName = playerName;
        this.serverId = serverId;
        this.messageHandler = messageHandler;
    }

    @Override
    public void onOpen(ServerHandshake handshakeData) {
        send(String.format("{\"playerName\":%s,\"serverId\":%s}", playerName, serverId));
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        NiamodClient.LOGGER.warn("Websocket connection closed with code {}, reason {}", code, reason);
    }

    @Override
    public void onMessage(String message) {
        this.messageHandler.onMessage(message);
        NiamodClient.LOGGER.info("Websocket message received: {}", message);
    }

    @Override
    public void onError(Exception ex) {
        NiamodClient.LOGGER.error("Websocket connection error", ex);
    }

    @FunctionalInterface
    public interface MessageHandler {
        void onMessage(String message);
    }
}