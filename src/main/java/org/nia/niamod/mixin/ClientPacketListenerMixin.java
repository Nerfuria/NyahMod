package org.nia.niamod.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.Component;
import org.nia.niamod.models.events.ChatEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ClientPacketListener.class, remap = false)
public class ClientPacketListenerMixin {
    @WrapOperation(method = "handleSystemChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/chat/ChatListener;handleSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    public void wrap(ChatListener instance, Component component, boolean bl, Operation<Void> original) {
        Component modified = ChatEvent.MODIFY.invoker().modifyMessage(component);
        if (modified == null) return;
        ChatEvent.RECIEVED.invoker().onMessage(modified);
        original.call(instance, modified, bl);
    }
}
