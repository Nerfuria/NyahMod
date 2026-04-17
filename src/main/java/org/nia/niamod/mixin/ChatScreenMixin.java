package org.nia.niamod.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import org.nia.niamod.eventbus.NiaEventBus;
import org.nia.niamod.models.events.CommandSentEvent;
import org.nia.niamod.models.misc.ExecuteRunnableClickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @Inject(method = "handleComponentClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/ChatScreen;defaultHandleGameClickEvent(Lnet/minecraft/network/chat/ClickEvent;Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/screens/Screen;)V", shift = At.Shift.BEFORE), cancellable = true)
    public void clicked(Style style, boolean bl, CallbackInfoReturnable<Boolean> cir, @Local ClickEvent clickEvent) {
        if (clickEvent instanceof ExecuteRunnableClickEvent(Runnable run)) {
            run.run();
            cir.setReturnValue(true);
        }
    }

    @Inject(
            method = "handleChatInput",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;sendCommand(Ljava/lang/String;)V",
                    shift = At.Shift.BEFORE
            ),
            cancellable = true
    )
    private void handleChatInput(String message, boolean addToRecentChat, CallbackInfo ci) {
        NiaEventBus.dispatch(new CommandSentEvent(message.substring(1)), cancelled -> ci.cancel());
    }
}
