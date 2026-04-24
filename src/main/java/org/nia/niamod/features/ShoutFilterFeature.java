package org.nia.niamod.features;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.eventbus.NiaEventBus;
import org.nia.niamod.eventbus.Subscribe;
import org.nia.niamod.mixin.ChatComponentAccessor;
import org.nia.niamod.models.config.ShoutReplacement;
import org.nia.niamod.models.events.ChatModifyEvent;
import org.nia.niamod.models.misc.ExecuteRunnableClickEvent;
import org.nia.niamod.models.misc.Feature;
import org.nia.niamod.models.misc.Safe;

import java.util.List;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class ShoutFilterFeature extends Feature {
    private Pattern shout;

    @Override
    @Safe
    public void init() {
        NiaEventBus.subscribe(this);
        shout = Pattern.compile("((\\uDAFF\\uDFFC\\uE015\\uDAFF\\uDFFF\\uE002\\uDAFF\\uDFFE)|(\\uDAFF\\uDFFC\\uE001\\uDB00\\uDC06)) [_A-Za-z0-9]{3,16} .+? shouts: .+");
    }

    @Subscribe
    public void modifyChat(ChatModifyEvent event) {
        Component component = event.getMessage();
        if (!NyahConfig.getData().isShoutFilterFeatureEnabled()) return;

        if (!shout.matcher(component.getString()).matches()) return;

        if (NyahConfig.getData().getShoutFilterMode() == ShoutReplacement.REMOVE) {
            event.setMessage(null);
        } else if (NyahConfig.getData().getShoutFilterMode() == ShoutReplacement.GRAY_OUT) {
            event.setMessage(withColor(component));
        } else {
            Minecraft mc = Minecraft.getInstance();
            final int insertTick = mc.gui.getGuiTicks();

             MutableComponent toReplace = Component.literal("Shout hidden, click to open");

             toReplace.withStyle(Style.EMPTY
                     .withColor(TextColor.fromRgb(0x3b1344))
                     .withClickEvent(new ExecuteRunnableClickEvent(() -> {
                         List<GuiMessage> allMessages = ((ChatComponentAccessor) mc.gui.getChat()).niamod$allMessages();
                         for (int i = 0; i < allMessages.size(); i++) {
                             if (allMessages.get(i).content().equals(toReplace)) {
                                 allMessages.set(i, new GuiMessage(insertTick, component, null, GuiMessageTag.system()));
                                 ((ChatComponentAccessor) mc.gui.getChat()).niamod$refreshTrimmedMessages();
                                 return;
                             }
                         }
                     })));

            event.setMessage(toReplace);
        }
    }

    private Component withColor(Component component) {
        MutableComponent copy = component.plainCopy();

        copy.setStyle(component.getStyle().withColor(TextColor.fromRgb(0x676767)));

        for (Component sibling : component.getSiblings()) {
            copy.append(withColor(sibling));
        }

        return copy;
    }

}
