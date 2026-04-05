package org.nia.niamod.mixin;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(ChatComponent.class)
public interface ChatComponentAccessor {

    @Accessor("allMessages")
    List<GuiMessage> niamod$allMessages();

    @Accessor("allMessages")
    void niamod$allMessages(List<GuiMessage> allMessages);

    @Invoker("refreshTrimmedMessages")
    void niamod$refreshTrimmedMessages();

}
