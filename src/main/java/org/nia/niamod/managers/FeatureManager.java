package org.nia.niamod.managers;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import org.nia.niamod.features.ChatEncryptionFeature;
import org.nia.niamod.features.ConsuTextFeature;
import org.nia.niamod.features.IgnoreFeature;
import org.nia.niamod.features.ResourceTickFeature;
import org.nia.niamod.features.WarTimersFeature;
import org.nia.niamod.features.WarTowerEHPFeature;
import org.nia.niamod.models.events.PostInitEvent;
import org.nia.niamod.models.misc.Feature;
import org.nia.niamod.models.misc.Safe;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.nia.niamod.NiamodClient.LOGGER;

public class FeatureManager {
    private static ResourceTickFeature resTickFeature;
    private static ChatEncryptionFeature chatEncryptionFeature;
    private static WarTimersFeature warTimersFeature;
    private static IgnoreFeature ignoreFeature;
    private static WarTowerEHPFeature warTowerEHPFeature;
    private static ConsuTextFeature consuTextFeature;

    public static void init() {
        resTickFeature = Feature.createSafe(ResourceTickFeature.class);
        chatEncryptionFeature = Feature.createSafe(ChatEncryptionFeature.class);
        warTimersFeature = Feature.createSafe(WarTimersFeature.class);
        ignoreFeature = Feature.createSafe(IgnoreFeature.class);
        warTowerEHPFeature = Feature.createSafe(WarTowerEHPFeature.class);
        consuTextFeature = Feature.createSafe(ConsuTextFeature.class);

        consuTextFeature.init();
        chatEncryptionFeature.init();
        warTimersFeature.init();
        resTickFeature.init();
        ignoreFeature.init();

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
}
