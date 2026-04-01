package org.nia.niamod.managers;

import org.nia.niamod.features.ChatEncryptionFeature;
import org.nia.niamod.features.ConsuTextFeature;
import org.nia.niamod.features.IgnoreFeature;
import org.nia.niamod.features.ResourceTickFeature;
import org.nia.niamod.features.WarTimersFeature;
import org.nia.niamod.features.WarTowerEHPFeature;

public class Features {
    private static ResourceTickFeature resTickFeature;
    private static ChatEncryptionFeature chatEncryptionFeature;
    private static WarTimersFeature warTimersFeature;
    private static IgnoreFeature ignoreFeature;
    private static WarTowerEHPFeature warTowerEHPFeature;
    private static ConsuTextFeature consuTextFeature;

    public static void init() {
        resTickFeature = new ResourceTickFeature();
        chatEncryptionFeature = new ChatEncryptionFeature();
        warTimersFeature = new WarTimersFeature();
        ignoreFeature = new IgnoreFeature();
        warTowerEHPFeature = new WarTowerEHPFeature();
        consuTextFeature = new ConsuTextFeature();

        chatEncryptionFeature.init();
        warTimersFeature.init();
        consuTextFeature.init();
    }

    public static void postInit() {
        ignoreFeature.init();
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

    public static ConsuTextFeature getConsuTextFeature() {
        return consuTextFeature;
    }
}
