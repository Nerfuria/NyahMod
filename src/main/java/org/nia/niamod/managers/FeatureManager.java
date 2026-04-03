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
        resTickFeature = createSafe(ResourceTickFeature.class);
        chatEncryptionFeature = createSafe(ChatEncryptionFeature.class);
        warTimersFeature = createSafe(WarTimersFeature.class);
        ignoreFeature = createSafe(IgnoreFeature.class);
        warTowerEHPFeature = createSafe(WarTowerEHPFeature.class);
        consuTextFeature = createSafe(ConsuTextFeature.class);

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

    /** Handle methods annotated with @Safe **/
    @SuppressWarnings("unchecked")
    public static <T extends Feature> T createSafe(Class<T> clazz) {
        ProxyFactory factory = new ProxyFactory();
        factory.setSuperclass(clazz);
        factory.setFilter(method ->
                method.getDeclaringClass().equals(clazz) && method.isAnnotationPresent(Safe.class)
        );
        MethodHandler handler = (self, thisMethod, proceed, args) -> {
            Object defaultValue = getDefault(thisMethod, args);
            Feature feature = (T) self;
            if (!feature.isEnabled()) return defaultValue;
            try {
                return proceed.invoke(self, args);
            } catch (Exception e) {
                LOGGER.error("Feature {} has crashed and is being disabled.", feature.getFeatureName(), e);
                feature.setEnabled(false);
                return defaultValue;
            }
        };
        try {
            return (T) factory.create(new Class<?>[0], new Object[0], handler);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object getDefault(Method method, Object[] args) {
        Safe annotation = method.getAnnotation(Safe.class);
        if (annotation == null) return null;
        int idx = annotation.ordinal();
        if (idx >= 0 && args != null && idx < args.length) return args[idx];
        return null;
    }
}
