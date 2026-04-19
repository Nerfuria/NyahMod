package org.nia.niamod.eventbus;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;

@UtilityClass
public class NiaEventBus {
    final EventBus EVENT_BUS = new EventBus(task -> Minecraft.getInstance().execute(task));

    public void subscribe(Object listener) {
        EVENT_BUS.subscribe(listener);
    }

    public void unsubscribe(Object listener) {
        EVENT_BUS.unsubscribe(listener);
    }

    public void dispatch(Object event) {
        EVENT_BUS.dispatch(event);
    }

    public void dispatch(Object event, CancelableCallback callback) {
        EVENT_BUS.dispatch(event, callback);
    }
}
