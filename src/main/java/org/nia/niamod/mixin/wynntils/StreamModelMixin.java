package org.nia.niamod.mixin.wynntils;

import com.wynntils.models.worlds.StreamerModeModel;
import org.nia.niamod.eventbus.NiaEventBus;
import org.nia.niamod.models.events.StreamEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StreamerModeModel.class)
public class StreamModelMixin {

    @Inject(method = "setStreamerMode", at = @At("RETURN"))
    public void onStream(boolean inStream, CallbackInfo ci) {
        NiaEventBus.dispatch(new StreamEvent());
    }
}
