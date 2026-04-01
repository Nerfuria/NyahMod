package org.nia.niamod.mixin;

import net.minecraft.entity.boss.BossBar;
import net.minecraft.text.Text;
import org.nia.niamod.managers.Features;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(BossBar.class)
public class BossBarHudMixin {

    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static Text onInit(Text value) {
        return Features.getWarTowerEHPFeature().replaceEHP(value);
    }

    @ModifyVariable(method = "setName", at = @At("HEAD"), argsOnly = true)
    private Text setName(Text value) {
        return Features.getWarTowerEHPFeature().replaceEHP(value);
    }
}