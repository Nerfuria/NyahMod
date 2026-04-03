package org.nia.niamod.models.misc;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.nia.niamod.NiamodClient.LOGGER;

public abstract class Feature {
    @Getter
    @Setter
    protected boolean enabled = true;

    /**
     * Handle methods annotated with @Safe
     **/
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
            if (feature.isDisabled()) return defaultValue;
            try {
                return proceed.invoke(self, args);
            } catch (InvocationTargetException e) {
                LOGGER.error("Feature {} has crashed and is being disabled.", feature.getFeatureName(), e.getCause());
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

    public abstract void init();

    public String getFeatureName() {
        return getClass().getSimpleName();
    }

    public boolean isDisabled() {
        return !enabled;
    }
}
