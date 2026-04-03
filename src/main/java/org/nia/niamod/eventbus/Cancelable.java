package org.nia.niamod.eventbus;

public interface Cancelable {
    default void cancel() {
        CancelableState.cancel(this);
    }
}
