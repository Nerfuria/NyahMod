package org.nia.niamod.features;

import com.wynntils.core.text.StyledText;
import com.wynntils.utils.wynn.ContainerUtils;
import com.wynntils.utils.wynn.InventoryUtils;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;

import java.util.regex.Pattern;

public class GuTerrFeature {
    private static final Pattern COMPASS_TITLE_PATTERN = Pattern.compile(".+\uDAFF\uDFDC\uE003");
    private static final Pattern GUILD_MANAGE_TITLE_PATTERN = Pattern.compile(".+: Manage");
    private static final Pattern GUILD_ECO_TITLE_PATTERN = Pattern.compile(".+: Territories");
    //Territory Management
    private static final int COMPASS_SLOT = 43;
    private static final int GUILD_MANAGE_SLOT = 26;
    private static final int GUILD_ECO_SLOT = 14;
    private boolean openTerritory = false;

    public void init() {
        ClientSendMessageEvents.ALLOW_COMMAND.register(this::processCommand);
        ScreenEvents.BEFORE_INIT.register(this::onBeforeScreenOpen);
    }

    private boolean processCommand(String message) {
        if (!message.startsWith("gu territory")) return true;

        openTerritory = true;
        InventoryUtils.sendInventorySlotMouseClick(
                COMPASS_SLOT, InventoryUtils.MouseClickType.LEFT_CLICK);

        return false;
    }

    private void onBeforeScreenOpen(MinecraftClient client, Screen screen, int scaledWidth, int scaledHeight) {
        if (!openTerritory) return;
        System.out.println("Screen: " + screen.getTitle().getString());
        openTerritory = false;

        StyledText title = StyledText.fromComponent(screen.getTitle());
        if (title.matches(COMPASS_TITLE_PATTERN)) {
            if (screen instanceof HandledScreen<?> handled) {
                ScreenHandler handler = handled.getScreenHandler();
                openTerritory = true;
                ContainerUtils.clickOnSlot(
                        GUILD_MANAGE_SLOT,
                        handler.syncId,
                        0,
                        handler.getStacks()
                );
            }
        } else if (title.matches(GUILD_MANAGE_TITLE_PATTERN)) {
            if (screen instanceof HandledScreen<?> handled) {
                ScreenHandler handler = handled.getScreenHandler();
                openTerritory = true;
                ContainerUtils.shiftClickOnSlot(
                        GUILD_ECO_SLOT,
                        handler.syncId,
                        0,
                        handler.getStacks()
                );
            }
        } else if (title.matches(GUILD_ECO_TITLE_PATTERN)) {
            if (screen instanceof HandledScreen<?> handled) {
                ScreenHandler handler = handled.getScreenHandler();
                // Click on terr?
                System.out.println("Open terr x");
            }
        }
    }
}
