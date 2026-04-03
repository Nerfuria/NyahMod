package org.nia.niamod.eventbus;

@FunctionalInterface
public interface CancelableCallback {
    void canceled(Object event);
}
