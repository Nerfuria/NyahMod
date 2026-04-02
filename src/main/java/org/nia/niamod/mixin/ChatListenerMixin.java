package org.nia.niamod.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.text.Text;
import org.nia.niamod.models.events.ChatEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ClientPlayNetworkHandler.class)
public class ChatListenerMixin {
    @ModifyArg(method = "onGameMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/message/MessageHandler;onGameMessage(Lnet/minecraft/text/Text;Z)V"), index = 0)
    public Text modifyText(Text message, boolean overlay) {
        if (overlay) return message;
        Text modified = ChatEvent.MODIFY.invoker().modifyMessage(message);
        ChatEvent.RECIEVED.invoker().onMessage(modified);
        return modified;
    }
}
