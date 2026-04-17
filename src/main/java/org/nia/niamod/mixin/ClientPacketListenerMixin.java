package org.nia.niamod.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.Component;
import org.nia.niamod.eventbus.NiaEventBus;
import org.nia.niamod.models.events.ChatMessageReceivedEvent;
import org.nia.niamod.models.events.ChatModifyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ClientPacketListener.class, remap = false)
public class ClientPacketListenerMixin {
    @WrapOperation(method = "handleSystemChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/chat/ChatListener;handleSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    public void wrap(ChatListener instance, Component component, boolean bl, Operation<Void> original) {
        if (bl) {
            original.call(instance, component, bl);
            return;
        }
        ChatModifyEvent event = new ChatModifyEvent(component);
        NiaEventBus.dispatch(event);
        Component modified = event.getMessage();
        if (modified == null) return;
        NiaEventBus.dispatch(new ChatMessageReceivedEvent(modified));
        original.call(instance, modified, bl);
    }
}
