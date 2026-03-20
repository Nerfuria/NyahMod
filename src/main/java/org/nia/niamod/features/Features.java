package org.nia.niamod.features;

public class Features {
    private static ResourceTickFeature resTickFeature;
    private static ChatEncryptionFeature chatEncryptionFeature;
    private static WarTimersFeature warTimersFeature;
    private static IgnoreFeature ignoreFeature;
    private static WarTowerEHPFeature warTowerEHPFeature;

    public static void init() {
        resTickFeature = new ResourceTickFeature();
        chatEncryptionFeature = new ChatEncryptionFeature();
        warTimersFeature = new WarTimersFeature();
        ignoreFeature = new IgnoreFeature();
        warTowerEHPFeature = new WarTowerEHPFeature();

        chatEncryptionFeature.init();
        warTimersFeature.init();
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

    public static IgnoreFeature getIgnoreFeature() {
        return ignoreFeature;
    }

    public static WarTowerEHPFeature getWarTowerEHPFeature() {
        return warTowerEHPFeature;
    }
}
