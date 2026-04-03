package org.nia.niamod.managers;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import org.nia.niamod.features.ChatEncryptionFeature;
import org.nia.niamod.features.ConsuTextFeature;
import org.nia.niamod.features.IgnoreFeature;
import org.nia.niamod.features.ResourceTickFeature;
import org.nia.niamod.features.ShoutFilterFeature;
import org.nia.niamod.features.WarTimersFeature;
import org.nia.niamod.features.WarTowerEHPFeature;
import org.nia.niamod.models.misc.Feature;

@SuppressWarnings("unused")
@UtilityClass
public class FeatureManager {
    @Getter
    private static ResourceTickFeature resTickFeature;
    @Getter
    private static ChatEncryptionFeature chatEncryptionFeature;
    @Getter
    private static WarTimersFeature warTimersFeature;
    @Getter
    private static IgnoreFeature ignoreFeature;
    @Getter
    private static WarTowerEHPFeature warTowerEHPFeature;
    @Getter
    private static ConsuTextFeature consuTextFeature;
    @Getter
    private static ShoutFilterFeature shoutFilterFeature;

    public static void init() {
        resTickFeature = Feature.createSafe(ResourceTickFeature.class);
        chatEncryptionFeature = Feature.createSafe(ChatEncryptionFeature.class);
        warTimersFeature = Feature.createSafe(WarTimersFeature.class);
        ignoreFeature = Feature.createSafe(IgnoreFeature.class);
        warTowerEHPFeature = Feature.createSafe(WarTowerEHPFeature.class);
        consuTextFeature = Feature.createSafe(ConsuTextFeature.class);
        shoutFilterFeature = Feature.createSafe(ShoutFilterFeature.class);

        resTickFeature.init();
        chatEncryptionFeature.init();
        warTimersFeature.init();
        ignoreFeature.init();
        warTowerEHPFeature.init();
        consuTextFeature.init();
        shoutFilterFeature.init();
    }
}
