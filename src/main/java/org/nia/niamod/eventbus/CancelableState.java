package org.nia.niamod.eventbus;

import java.util.concurrent.ConcurrentHashMap;

final class CancelableState {
    private static final ConcurrentHashMap<Cancelable, Boolean> CANCELED_STATE = new ConcurrentHashMap<>();

    private CancelableState() {
    }

    static void cancel(Cancelable cancelable) {
        CANCELED_STATE.putIfAbsent(cancelable, true);
    }

    static boolean isCanceled(Cancelable cancelable) {
        Boolean canceled = CANCELED_STATE.get(cancelable);
        return canceled != null && canceled;
    }

    static void clear(Cancelable cancelable) {
        CANCELED_STATE.remove(cancelable);
    }
}
