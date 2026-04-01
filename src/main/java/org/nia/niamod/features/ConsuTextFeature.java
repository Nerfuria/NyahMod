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
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.models.events.SlotRenderEvent;
import org.nia.niamod.models.misc.Feature;
import org.nia.niamod.models.records.StatLabel;
import org.nia.niamod.util.FileUtils;

import java.util.List;
import java.util.Optional;

public class ConsuTextFeature extends Feature {

    private List<StatLabel> STAT_LABELS;

    protected void init() {
        STAT_LABELS = parseStatLabels();
        SlotRenderEvent.EVENT.register(((DrawContext context, ItemStack stack, int slotX, int slotY) -> runSafe(() -> renderText(context, stack, slotX, slotY))));
    }

    private List<StatLabel> parseStatLabels() {
        String json = FileUtils.readFile("stat_labels.json");
        return new Gson().fromJson(json, new TypeToken<List<StatLabel>>() {
        }.getType());
    }

    public void renderText(DrawContext context, ItemStack stack, int slotX, int slotY) {
        Optional<WynnItem> item = Models.Item.getWynnItem(stack);
        if (item.isEmpty()) return;
        WynnItem wynnItem = item.get();
        if (wynnItem instanceof CraftedConsumableItem consu) {
            String id = idsToText(consu.getIdentifications());
            if (id.isEmpty()) return;
            context.getMatrices().pushMatrix();
            context.getMatrices().scale(NyahConfig.nyahConfigData.idScale, NyahConfig.nyahConfigData.idScale);
            float x = (slotX + NyahConfig.nyahConfigData.idXOffset) / NyahConfig.nyahConfigData.idScale;
            float y = (slotY + NyahConfig.nyahConfigData.idYOffset) / NyahConfig.nyahConfigData.idScale;
            FontRenderer.getInstance().renderText(context, x, y, new TextRenderTask(StyledText.fromUnformattedString(id), TextRenderSetting.DEFAULT.withTextShadow(TextShadow.OUTLINE)));
            context.getMatrices().popMatrix();
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
