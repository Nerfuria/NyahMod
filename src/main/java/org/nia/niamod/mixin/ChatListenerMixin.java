package org.nia.niamod.mixin;

import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatListener.class)
public class ChatListenerMixin {
    @Inject(method = "handleSystemMessage", at = @At("HEAD"), cancellable = true)
    public void handleMessage(Component component, boolean bl, CallbackInfo ci) {
        if (component == null) ci.cancel();
    }
}
