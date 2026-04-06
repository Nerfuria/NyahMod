package org.nia.niamod.features;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.mixin.ChatComponentAccessor;
import org.nia.niamod.models.events.ChatEvent;
import org.nia.niamod.models.misc.ExecuteRunnableClickEvent;
import org.nia.niamod.models.misc.Feature;
import org.nia.niamod.models.misc.Safe;
import org.nia.niamod.models.misc.ShoutReplacement;

import java.util.List;

public class ShoutFilterFeature extends Feature {
    @Override
    @Safe
    public void init() {
        ChatEvent.MODIFY.register(this::modifyChat);
    }

    public Component modifyChat(Component component) {
        if (!NyahConfig.nyahConfigData.replaceShoutMessages) return component;
        if (!component.getString().startsWith("\uDAFF\uDFFC\uE015\uDAFF\uDFFF\uE002\uDAFF\uDFFE")) return component;
        if (NyahConfig.nyahConfigData.shoutFilterMode == ShoutReplacement.REMOVE) return null;
        if (NyahConfig.nyahConfigData.shoutFilterMode == ShoutReplacement.GRAY_OUT) return component.plainCopy()
                .setStyle(component.getStyle().withColor(TextColor.fromRgb(0x686868)));
        else {
            Minecraft mc = Minecraft.getInstance();
            int currentSize = ((ChatComponentAccessor) mc.gui.getChat()).niamod$allMessages().size() + 1;

            return Component.literal("Shout hidden, click to open.")
                    .setStyle(component.getStyle()
                            .withClickEvent(new ExecuteRunnableClickEvent(() -> {
                                List<GuiMessage> allMessages = ((ChatComponentAccessor) mc.gui.getChat()).niamod$allMessages();
                                allMessages.set(allMessages.size() - currentSize, new GuiMessage(mc.gui.getGuiTicks(), component, null, GuiMessageTag.system()));
                                ((ChatComponentAccessor) mc.gui.getChat()).niamod$allMessages(allMessages);
                                ((ChatComponentAccessor) mc.gui.getChat()).niamod$refreshTrimmedMessages();
                            })).withColor(TextColor.fromRgb(0x35063E))
                    );
        }
    }


}
