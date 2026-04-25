package org.nia.niamod.managers;

import org.nia.niamod.features.*;
import org.nia.niamod.models.events.PostInitEvent;
import org.nia.niamod.models.misc.Feature;

@SuppressWarnings("unused")
public class FeatureManager {
    private static ResourceTickFeature resTickFeature;
    private static ChatEncryptionFeature chatEncryptionFeature;
    private static WarTimersFeature warTimersFeature;
    private static IgnoreFeature ignoreFeature;
    private static WarTowerEHPFeature warTowerEHPFeature;
    private static ConsuTextFeature consuTextFeature;
    private static ShoutFilterFeature shoutFilterFeature;
    private static DefenseEstimatesFeature defenseEstimatesFeature;

    public static void init() {
        resTickFeature = Feature.createSafe(ResourceTickFeature.class);
        chatEncryptionFeature = Feature.createSafe(ChatEncryptionFeature.class);
        warTimersFeature = Feature.createSafe(WarTimersFeature.class);
        ignoreFeature = Feature.createSafe(IgnoreFeature.class);
        warTowerEHPFeature = Feature.createSafe(WarTowerEHPFeature.class);
        consuTextFeature = Feature.createSafe(ConsuTextFeature.class);
        shoutFilterFeature = Feature.createSafe(ShoutFilterFeature.class);
        defenseEstimatesFeature = Feature.createSafe(DefenseEstimatesFeature.class);

        resTickFeature.init();
        chatEncryptionFeature.init();
        warTimersFeature.init();
        ignoreFeature.init();
        warTowerEHPFeature.init();
        consuTextFeature.init();
        shoutFilterFeature.init();
        defenseEstimatesFeature.init();

        PostInitEvent.EVENT.register(FeatureManager::postInit);
    }

    public static void postInit() {
        ignoreFeature.postInit();
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

    public static ShoutFilterFeature getShoutFilterFeature() {
        return shoutFilterFeature;
    }

    public static DefenseEstimatesFeature getDefenseEstimatesFeature() {
        return defenseEstimatesFeature;
    }
}
