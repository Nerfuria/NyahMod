package org.nia.niamod.features;

import org.nia.niamod.NiamodClient;

public abstract class Feature {
    private boolean enabled = true;

    public final void initSafe() {
        runSafe(this::init);
    }

    protected abstract void init();

    public String getFeatureName() {
        return getClass().getSimpleName();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Safely run an action. If an exception is raised, the feature is disabled.
     **/
    public void runSafe(Runnable action) {
        if (!isEnabled()) return;
        try {
            action.run();
        } catch (Exception e) {
            NiamodClient.LOGGER.error("Feature {} has crashed and is being disabled.", getFeatureName(), e);
            setEnabled(false);
        }
    }

    /**
     * Safely run an action with return value. If an exception is raised, the feature is disabled.
     **/
    public <T> T runSafe(java.util.function.Supplier<T> action, T fallback) {
        if (!isEnabled()) return fallback;
        try {
            return action.get();
        } catch (Exception e) {
            NiamodClient.LOGGER.error("Feature {} has crashed and is being disabled.", getFeatureName(), e);
            setEnabled(false);
            return fallback;
        }
    }
}
