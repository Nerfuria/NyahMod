package org.nia.niamod.features;

public class Features {
    private static WebsocketFeature wsFeature;
    private static ChatEncryptionFeature chatEncryptionFeature;

    public static void init() {
        wsFeature = new WebsocketFeature();
        chatEncryptionFeature = new ChatEncryptionFeature();

        chatEncryptionFeature.init();
    }

    public static WebsocketFeature getWsFeature() {
        return wsFeature;
    }

    public static ChatEncryptionFeature getChatEncryptionFeature() {
        return chatEncryptionFeature;
    }
}
