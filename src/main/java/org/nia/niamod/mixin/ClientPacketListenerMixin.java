package org.nia.niamod.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import org.nia.niamod.models.events.ChatEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @ModifyArg(method = "handleSystemChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/chat/ChatListener;handleSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"), index = 0)
    public Component modifyText(Component message, boolean overlay) {
        if (overlay) return message;
        Component modified = ChatEvent.MODIFY.invoker().modifyMessage(message);
        if (modified == null) return null;
        ChatEvent.RECIEVED.invoker().onMessage(modified);
        return modified;
    }
}
