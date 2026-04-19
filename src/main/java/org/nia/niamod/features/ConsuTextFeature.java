package org.nia.niamod.features;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.wynntils.core.text.StyledText;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.TextRenderSetting;
import com.wynntils.utils.render.TextRenderTask;
import com.wynntils.utils.render.type.TextShadow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.eventbus.NiaEventBus;
import org.nia.niamod.eventbus.Subscribe;
import org.nia.niamod.models.events.SlotRenderEvent;
import org.nia.niamod.models.misc.ConsuType;
import org.nia.niamod.models.misc.Feature;
import org.nia.niamod.models.misc.Safe;
import org.nia.niamod.models.records.StatLabel;
import org.nia.niamod.util.FileUtils;

import java.util.List;

@SuppressWarnings("unused")
public class ConsuTextFeature extends Feature {

    private List<StatLabel> STAT_LABELS;

    @Safe
    public void init() {
        STAT_LABELS = parseStatLabels();
        NiaEventBus.subscribe(this);
    }

    private List<StatLabel> parseStatLabels() {
        String json = FileUtils.readFile("stat_labels.json");
        return new Gson().fromJson(json, new TypeToken<List<StatLabel>>() {
        }.getType());
    }

    @Subscribe
    @Safe
    public void renderText(SlotRenderEvent event) {
        GuiGraphics context = event.context();
        ItemStack stack = event.stack();
        int slotX = event.slotX();
        int slotY = event.slotY();
        LocalPlayer player = Minecraft.getInstance().player;
        List<Component> lore = stack.getTooltipLines(Item.TooltipContext.EMPTY, player, TooltipFlag.ADVANCED);
        if (getType(lore) == ConsuType.NONE) return;

        List<String> tooltip = lore
                .stream()
                .map(line -> line.getString().replaceAll("[^a-zA-Z %]", "").replaceAll("\\s+", " ").trim())
                .toList();

        int startIndex = -1;
        int endIndex = tooltip.size();
        for (int i = 0; i < tooltip.size(); i++) {
            if (tooltip.get(i).contains("Combat Level")) {
                startIndex = i + 2;
                break;
            }
        }
        if (startIndex == -1) return;
        for (int i = startIndex; i < tooltip.size(); i++) {
            if (tooltip.get(i).contains("Crafted by")) {
                endIndex = i;
                break;
            }
        }
        List<String> IDS = tooltip.stream()
                .skip(startIndex)
                .limit(endIndex - startIndex)
                .filter(line -> !line.isBlank())
                .toList();

        String id = idsToText(IDS);
        if (id.isEmpty()) return;
        context.pose().pushMatrix();
        context.pose().scale(NyahConfig.getData().getIdScale(), NyahConfig.getData().getIdScale());
        float x = (slotX + NyahConfig.getData().getIdXOffset()) / NyahConfig.getData().getIdScale();
        float y = (slotY + NyahConfig.getData().getIdYOffset()) / NyahConfig.getData().getIdScale();
        FontRenderer.getInstance().renderText(context, x, y, new TextRenderTask(StyledText.fromUnformattedString(id), TextRenderSetting.DEFAULT.withTextShadow(TextShadow.OUTLINE)));
        context.pose().popMatrix();
    }

    private String idsToText(List<String> statTypes) {
        for (StatLabel label : STAT_LABELS) {
            int matches = (int) label.ids().stream().filter(statTypes::contains).count();
            int required = label.minCount() != null ? label.minCount() : 1;
            if (matches < required) continue;
            return label.alias();
        }

        return "";
    }

    private ConsuType getType(List<Component> loreLines) {
        if (loreLines.stream().anyMatch(l -> l.getString().contains("\uE035\uDAFF\uDFFF\uE03E\uDAFF\uDFFF\uE03E\uDAFF\uDFFF\uE033\uDAFF\uDFFF\uE062\uDAFF\uDFE6\uE005\uE00E\uE00E\uE003\uDB00\uDC02")))
            return ConsuType.FOOD;
        if (loreLines.stream().anyMatch(l -> l.getString().contains("\uE042\uDAFF\uDFFF\uE032\uDAFF\uDFFF\uE041\uDAFF\uDFFF\uE03E\uDAFF\uDFFF\uE03B\uDAFF\uDFFF\uE03B\uDAFF\uDFFF\uE062\uDAFF\uDFDA\uE012\uE002\uE011\uE00E\uE00B\uE00B\uDB00\uDC02")))
            return ConsuType.SCROLL;
        if (loreLines.stream().anyMatch(l -> l.getString().contains("\uE03F\uDAFF\uDFFF\uE03E\uDAFF\uDFFF\uE043\uDAFF\uDFFF\uE038\uDAFF\uDFFF\uE03E\uDAFF\uDFFF\uE03D\uDAFF\uDFFF\uE062\uDAFF\uDFDC\uE00F\uE00E\uE013\uE008\uE00E\uE00D\uDB00\uDC02")))
            return ConsuType.POTION;
        return ConsuType.NONE;
    }

}
