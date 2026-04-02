package org.nia.niamod.models.misc;

public abstract class Feature {
    private boolean enabled = true;

    public abstract void init();

    public String getFeatureName() {
        return getClass().getSimpleName();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
