package org.nia.niamod.models.events;

import lombok.Value;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.nia.niamod.eventbus.EventInfo;
import org.nia.niamod.eventbus.Preference;

@Value
@EventInfo(preference = Preference.CALLER)
public class SlotRenderEvent {
    GuiGraphics context;
    ItemStack stack;
    int slotX;
    int slotY;
}
