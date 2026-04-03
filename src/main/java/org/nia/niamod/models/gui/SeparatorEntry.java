package org.nia.niamod.models.gui;

import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.nia.niamod.NiamodClient.mc;

public class SeparatorEntry extends TooltipListEntry<Component> {
    private final int separatorLength;
    private final Component name;

    public SeparatorEntry(Component fieldName, @Nullable Supplier<Optional<Component[]>> tooltipSupplier) {
        super(fieldName, tooltipSupplier);
        separatorLength = mc.font.width(fieldName);
        this.name = fieldName;
    }

    @Override
    public Optional<Component> getDefaultValue() {
        return Optional.empty();
    }

    @Override
    public Component getValue() {
        return null;
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return List.of();
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return List.of();
    }

    @Override
    public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        graphics.drawString(
                mc.font,
                name,
                x + (entryWidth - separatorLength) / 2,
                y + 6,
                getPreferredTextColor(),
                true
        );
    }
}
