package org.nia.niamod.models.gui;

import com.google.common.collect.Lists;
import lombok.Setter;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import org.nia.niamod.models.records.State;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.nia.niamod.NiamodClient.mc;

public class IgnoreEntry extends TooltipListEntry<Component> {

    private static final int BUTTON_WIDTH = 80;

    private final Button ignoreWidget;
    private final MultiClickButton favouriteWidget;
    private final List<AbstractButton> widgets;

    private final int emojiWidth;
    private final int startWidth;

    @Setter
    private boolean edited = false;

    public IgnoreEntry(
            Component fieldName,
            @Nullable Supplier<Optional<Component[]>> tooltipSupplier,
            Consumer<Button> onIgnore,
            Consumer<Button> onFavourite,
            Consumer<Button> onAvoid,
            Consumer<Button> onReset,
            State state
    ) {
        super(fieldName, tooltipSupplier);

        var renderer = mc.font;
        emojiWidth = renderer.width("♥");
        startWidth = renderer.width("@@@@@@@@@@@@@@@@");

        ignoreWidget = Button.builder(Component.nullToEmpty("§2Ignore"), onIgnore::accept)
                .bounds(0, 0, BUTTON_WIDTH, 20)
                .build();

        favouriteWidget = MultiClickButton
                .builder(
                        Component.nullToEmpty(state.getCode() + "♥"),
                        markThen(onFavourite),
                        markThen(onAvoid),
                        markThen(onReset),
                        false)
                .tooltip(Tooltip.create(Component.nullToEmpty("Left click to favourite, Right click to avoid, Middle click to reset")))
                .dimensions(0, 0, emojiWidth + 8, 20)
                .build();

        widgets = Lists.newArrayList(ignoreWidget, favouriteWidget);
    }

    private Consumer<Button> markThen(Consumer<Button> action) {
        return button -> {
            setEdited(true);
            action.accept(button);
        };
    }

    @Override
    public boolean isEdited() {
        return edited;
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
    public Iterator<String> getSearchTags() {
        return Collections.singleton(getFieldName().toString()).iterator();
    }


    @Override
    public List<? extends NarratableEntry> narratables() {
        return widgets;
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return widgets;
    }

    @Override
    public void render(GuiGraphics graphics, int index, int y, int x,
                       int entryWidth, int entryHeight,
                       int mouseX, int mouseY, boolean isHovered, float delta) {
        super.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);

        var renderer = mc.font;
        graphics.drawString(renderer, getDisplayedFieldName(), x, y + 6, getPreferredTextColor(), true);

        ignoreWidget.setPosition(x + startWidth + 4, y);
        ignoreWidget.setWidth(BUTTON_WIDTH);
        ignoreWidget.setFocused(ignoreWidget.isMouseOver(mouseX, mouseY));
        favouriteWidget.setPosition(x - emojiWidth - 10, y);

        favouriteWidget.render(graphics, mouseX, mouseY, delta);
        ignoreWidget.render(graphics, mouseX, mouseY, delta);
    }
}
