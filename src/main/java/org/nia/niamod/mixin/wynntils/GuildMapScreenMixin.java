package org.nia.niamod.mixin.wynntils;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.territories.type.TerritoryUpgrade;
import com.wynntils.screens.maps.GuildMapScreen;
import com.wynntils.services.map.pois.TerritoryPoi;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.ChatFormatting;
import org.nia.niamod.features.DefenseEstimatesFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(GuildMapScreen.class)
public class GuildMapScreenMixin {
    @Inject(method = "renderTerritoryTooltip", at = @At(ordinal = 4, value = "INVOKE", target = "Lcom/wynntils/utils/render/FontRenderer;renderText(Lnet/minecraft/client/gui/GuiGraphics;Lcom/wynntils/core/text/StyledText;FFLcom/wynntils/utils/colors/CustomColor;Lcom/wynntils/utils/render/type/HorizontalAlignment;Lcom/wynntils/utils/render/type/VerticalAlignment;Lcom/wynntils/utils/render/type/TextShadow;)V", shift = At.Shift.AFTER))
    private static void renderEstimatedDefenses(
            GuiGraphics guiGraphics,
            int xOffset,
            int yOffset,
            TerritoryPoi territoryPoi,
            CallbackInfo ci,
            @Local(name = "renderYOffset") LocalFloatRef renderYOffset) {
        Map<TerritoryUpgrade, Integer> estimates = DefenseEstimatesFeature.estimateDefenses(territoryPoi.getName());

        for (TerritoryUpgrade upgrade : TerritoryUpgrade.values()) {
            int level = estimates.getOrDefault(upgrade, 0);
            if (level <= 0) continue;

            renderYOffset.set(renderYOffset.get() + 10.0F);
            FontRenderer.getInstance()
                    .renderText(
                            guiGraphics,
                            StyledText.fromString(ChatFormatting.GRAY + "Estimated " + upgrade.getName() + ": " + ChatFormatting.WHITE + level),
                            10 + xOffset,
                            10.0F + renderYOffset.get(),
                            CommonColors.WHITE,
                            HorizontalAlignment.LEFT,
                            VerticalAlignment.TOP,
                            TextShadow.OUTLINE);
        }
    }

    @ModifyVariable(method = "renderTerritoryTooltip", at = @At("STORE"), name = "centerHeight")
    private static float increaseTooltipHeight(float centerHeight, @Local(argsOnly = true) TerritoryPoi territoryPoi) {
        return centerHeight + (DefenseEstimatesFeature.estimateDefenses(territoryPoi.getName()).entrySet().stream().filter(e -> e.getValue() != 0).count() * 10.0F);
    }
}
