package org.nia.niamod.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import org.nia.niamod.eventbus.NiaEventBus;
import org.nia.niamod.models.events.ChatMessageReceivedEvent;
import org.nia.niamod.models.events.ChatModifyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ClientPacketListener.class)
public class ChatListenerMixin {
    @ModifyArg(method = "handleSystemChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/chat/ChatListener;handleSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"), index = 0)
    public Component modifyText(Component message, boolean overlay) {
        if (overlay) return message;
        ChatModifyEvent event = new ChatModifyEvent(message);
        NiaEventBus.dispatch(event);
        Component modified = event.getMessage();
        NiaEventBus.dispatch(new ChatMessageReceivedEvent(modified));
        return modified;
    }
}
