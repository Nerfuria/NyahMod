package org.nia.niamod.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import org.nia.niamod.eventbus.NiaEventBus;
import org.nia.niamod.models.events.CommandSentEvent;
import org.nia.niamod.models.misc.ExecuteRunnableClickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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

    @WrapOperation(
            method = "handleChatInput",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;sendCommand(Ljava/lang/String;)V"
            )
    )
    private void handleChatInput(ClientPacketListener instance, String command, Operation<Void> original) {
        boolean[] canceled = {false};
        CommandSentEvent event = new CommandSentEvent(command);
        NiaEventBus.dispatch(event, ignored -> canceled[0] = true);
        if (!canceled[0]) {
            original.call(instance, event.getCommand());
        }
    }
}
