package org.nia.niamod.managers;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.features.ChatEncryptionFeature;
import org.nia.niamod.features.ConsuTextFeature;
import org.nia.niamod.features.GlobalChatFeature;
import org.nia.niamod.features.RadianceSyncFeature;
import org.nia.niamod.features.ResourceTickFeature;
import org.nia.niamod.features.ShoutFilterFeature;
import org.nia.niamod.features.ViewModelTransformationFeature;
import org.nia.niamod.features.WarTimersFeature;
import org.nia.niamod.features.WarTowerEHPFeature;

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
    private static WarTowerEHPFeature warTowerEHPFeature;
    @Getter
    private static ConsuTextFeature consuTextFeature;
    @Getter
    private static ShoutFilterFeature shoutFilterFeature;
    @Getter
    private static ViewModelTransformationFeature viewModelTransformationFeature;
    @Getter
    private static GlobalChatFeature globalChatFeature;
    @Getter
    private static RadianceSyncFeature radianceSyncFeature;

    public static void init() {
        resTickFeature = new ResourceTickFeature();
        chatEncryptionFeature = new ChatEncryptionFeature();
        warTimersFeature = new WarTimersFeature();
        warTowerEHPFeature = new WarTowerEHPFeature();
        consuTextFeature = new ConsuTextFeature();
        shoutFilterFeature = new ShoutFilterFeature();
        viewModelTransformationFeature = new ViewModelTransformationFeature();
        globalChatFeature = new GlobalChatFeature();
        radianceSyncFeature = new RadianceSyncFeature();

        resTickFeature.runSafe("init", resTickFeature::init);
        chatEncryptionFeature.runSafe("init", chatEncryptionFeature::init);
        warTimersFeature.runSafe("init", warTimersFeature::init);
        ignoreFeature.runSafe("init", ignoreFeature::init);
        warTowerEHPFeature.runSafe("init", warTowerEHPFeature::init);
        consuTextFeature.runSafe("init", consuTextFeature::init);
        shoutFilterFeature.runSafe("init", shoutFilterFeature::init);
        viewModelTransformationFeature.runSafe("init", viewModelTransformationFeature::init);
        globalChatFeature.runSafe("init", globalChatFeature::init);
        radianceSyncFeature.runSafe("init", radianceSyncFeature::init);

        NyahConfig.onFeaturesInitialized();
    }
}
