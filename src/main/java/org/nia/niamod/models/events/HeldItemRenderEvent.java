package org.nia.niamod.models.events;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.InteractionHand;
import org.nia.niamod.eventbus.EventInfo;
import org.nia.niamod.eventbus.Preference;

@EventInfo(preference = Preference.CALLER)
public record HeldItemRenderEvent(InteractionHand hand, PoseStack matrix) {
}
