package org.nia.niamod.features;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.eventbus.Subscribe;
import org.nia.niamod.mixin.ChatComponentAccessor;
import org.nia.niamod.models.config.ShoutReplacement;
import org.nia.niamod.models.events.ChatModifyEvent;
import org.nia.niamod.models.misc.ExecuteRunnableClickEvent;
import org.nia.niamod.models.misc.Feature;
import org.nia.niamod.models.misc.Safe;

import java.util.List;

public class ShoutFilterFeature extends Feature {
    @Override
    @Safe
    public void init() {
    }

    @Subscribe
    public void modifyChat(ChatModifyEvent event) {
        Component component = event.getMessage();
        if (!NyahConfig.nyahConfigData.isShoutReplacementFeatureEnabled()) return;
        if (!component.getString().startsWith("\uDAFF\uDFFC\uE015\uDAFF\uDFFF\uE002\uDAFF\uDFFE")) return;
        if (NyahConfig.nyahConfigData.getShoutFilterMode() == ShoutReplacement.REMOVE) event.setMessage(null);
        if (NyahConfig.nyahConfigData.getShoutFilterMode() == ShoutReplacement.GRAY_OUT)
            event.setMessage(Component.empty().append(component)
                    .setStyle(component.getStyle().withColor(TextColor.fromRgb(0x686868))));
        else {
            Minecraft mc = Minecraft.getInstance();
            int currentSize = ((ChatComponentAccessor) mc.gui.getChat()).niamod$allMessages().size() + 1;

            event.setMessage(Component.literal("Shout hidden, click to open.")
                    .setStyle(component.getStyle()
                            .withClickEvent(new ExecuteRunnableClickEvent(() -> {
                                List<GuiMessage> allMessages = ((ChatComponentAccessor) mc.gui.getChat()).niamod$allMessages();
                                allMessages.set(allMessages.size() - currentSize, new GuiMessage(mc.gui.getGuiTicks(), component, null, GuiMessageTag.system()));
                                ((ChatComponentAccessor) mc.gui.getChat()).niamod$refreshTrimmedMessages();
                            })).withColor(TextColor.fromRgb(0x35063E))
                    ));
        }
    }


}
