package org.nia.niamod.features;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.items.WynnItem;
import com.wynntils.models.items.items.game.CraftedConsumableItem;
import com.wynntils.models.stats.type.StatActualValue;
import com.wynntils.models.stats.type.StatType;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.TextRenderSetting;
import com.wynntils.utils.render.TextRenderTask;
import com.wynntils.utils.render.type.TextShadow;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.eventbus.NiaEventBus;
import org.nia.niamod.eventbus.Subscribe;
import org.nia.niamod.models.events.SlotRenderEvent;
import org.nia.niamod.models.misc.Feature;
import org.nia.niamod.models.misc.Safe;
import org.nia.niamod.models.records.StatLabel;
import org.nia.niamod.util.FileUtils;

import java.util.List;
import java.util.Optional;

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
        Optional<WynnItem> item = Models.Item.getWynnItem(stack);
        if (item.isEmpty()) return;
        WynnItem wynnItem = item.get();
        if (wynnItem instanceof CraftedConsumableItem consu) {
            String id = idsToText(consu.getIdentifications());
            if (id.isEmpty()) return;
            context.pose().pushMatrix();
            context.pose().scale(NyahConfig.nyahConfigData.getIdScale(), NyahConfig.nyahConfigData.getIdScale());
            float x = (slotX + NyahConfig.nyahConfigData.getIdXOffset()) / NyahConfig.nyahConfigData.getIdScale();
            float y = (slotY + NyahConfig.nyahConfigData.getIdYOffset()) / NyahConfig.nyahConfigData.getIdScale();
            FontRenderer.getInstance().renderText(context, x, y, new TextRenderTask(StyledText.fromUnformattedString(id), TextRenderSetting.DEFAULT.withTextShadow(TextShadow.OUTLINE)));
            context.pose().popMatrix();
        }
    }

    private String idsToText(List<StatActualValue> ids) {
        List<String> statTypes = ids.stream()
                .map(StatActualValue::statType)
                .map(StatType::getApiName)
                .toList();

        for (StatLabel label : STAT_LABELS) {
            int matches = (int) label.ids().stream().filter(statTypes::contains).count();
            int required = label.minCount() != null ? label.minCount() : 1;

            if (matches < required) continue;

            if (label.alias().equals("{sign}ATK")) {
                return ids.stream()
                        .filter(s -> s.statType().getApiName().equals("rawAttackSpeed"))
                        .findFirst()
                        .map(s -> (s.value() < 0 ? "-" : "+") + "ATK")
                        .orElse("ATK");
            }

            return label.alias();
        }

        return "";
    }

}
