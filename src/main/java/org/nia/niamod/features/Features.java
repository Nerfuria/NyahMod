package org.nia.niamod.features;

public class Features {
    private static WebsocketFeature wsFeature;
    private static ResourceTickFeature resTickFeature;
    private static ChatEncryptionFeature chatEncryptionFeature;
    private static WarTimersFeature warTimersFeature;

    public static void init() {
        wsFeature = new WebsocketFeature();
        resTickFeature = new ResourceTickFeature();
        chatEncryptionFeature = new ChatEncryptionFeature();
        warTimersFeature = new WarTimersFeature();

        chatEncryptionFeature.init();
        warTimersFeature.init();
    }

    public static WebsocketFeature getWsFeature() {
        return wsFeature;
    }

    public static ResourceTickFeature getResTickFeature() {
        return resTickFeature;
    }

    public static ChatEncryptionFeature getChatEncryptionFeature() {
        return chatEncryptionFeature;
    }

    public static WarTimersFeature getWarTimersFeature() {
        return warTimersFeature;
    }
}
