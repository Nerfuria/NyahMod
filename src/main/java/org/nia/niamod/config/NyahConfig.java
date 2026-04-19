package org.nia.niamod.config;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;
import org.nia.niamod.NiamodClient;
import org.nia.niamod.config.setting.SettingSection;
import org.nia.niamod.managers.FeatureManager;
import org.nia.niamod.managers.KeybindManager;
import org.nia.niamod.models.config.SettingCategory;
import org.nia.niamod.models.gui.NiaClickGuiScreen;
import org.nia.niamod.models.gui.theme.ClickGuiFontOption;
import org.nia.niamod.models.gui.theme.ClickGuiThemeOption;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@UtilityClass
public class NyahConfig {
    private static final Gson GSON = new Gson();
    private static final List<SettingSection> SECTIONS = List.copyOf(NyahConfigSections.createSections());

    @Getter
    private static NyahConfigData data = new NyahConfigData();

    public static void init() {
        loadConfig();
        KeybindManager.registerKeybinding(
                "Open NiaMod Click GUI",
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                () -> minecraft().setScreen(getConfigScreen())
        );
    }

    public static void onFeaturesInitialized() {
        applyFeatureStates();
    }

    public static void reset() {
        data = new NyahConfigData();
        data.normalize();
        save();
        applyFeatureStates();
    }

    public static Screen getConfigScreen() {
        return getConfigScreen(minecraft().screen);
    }

    public static Screen getConfigScreen(Screen currentScreen) {
        return new NiaClickGuiScreen(currentScreen);
    }

    public static List<SettingSection> getSections(SettingCategory category) {
        return SECTIONS.stream()
                .filter(section -> section.category() == category)
                .toList();
    }

    public static ClickGuiThemeOption getClickGuiThemeOption() {
        return ClickGuiThemeOption.resolve(getData().getClickGuiTheme());
    }

    public static ClickGuiFontOption getClickGuiFontOption() {
        return ClickGuiFontOption.resolve(getData().getClickGuiFont());
    }

    public static void save() {
        try {
            Files.createDirectories(configDir());
            try (Writer writer = Files.newBufferedWriter(configFile())) {
                GSON.toJson(data, writer);
            }
        } catch (IOException exception) {
            NiamodClient.LOGGER.error("Failed to save config", exception);
        }
    }

    public static void applyFeatureStates() {
        NyahConfigData config = getData();
        if (FeatureManager.getResTickFeature() != null) {
            FeatureManager.getResTickFeature().setEnabled(config.isResourceTickFeatureEnabled());
        }
        if (FeatureManager.getChatEncryptionFeature() != null) {
            FeatureManager.getChatEncryptionFeature().setEnabled(config.isChatEncryptionFeatureEnabled());
        }
        if (FeatureManager.getWarTimersFeature() != null) {
            FeatureManager.getWarTimersFeature().setEnabled(config.isWarTimersFeatureEnabled());
        }
        if (FeatureManager.getWarTowerEHPFeature() != null) {
            FeatureManager.getWarTowerEHPFeature().setEnabled(config.isWarTowerEhpFeatureEnabled());
        }
        if (FeatureManager.getConsuTextFeature() != null) {
            FeatureManager.getConsuTextFeature().setEnabled(config.isConsuTextFeatureEnabled());
        }
        if (FeatureManager.getShoutFilterFeature() != null) {
            FeatureManager.getShoutFilterFeature().setEnabled(config.isShoutFilterFeatureEnabled());
        }
        if (FeatureManager.getViewModelTransformationFeature() != null) {
            FeatureManager.getViewModelTransformationFeature().setEnabled(config.isViewModelFeatureEnabled());
        }
        if (FeatureManager.getRadianceSyncFeature() != null) {
            FeatureManager.getRadianceSyncFeature().setEnabled(config.isRadianceSyncEnabled());
        }
        if (FeatureManager.getAutoStreamFeature() != null) {
            FeatureManager.getAutoStreamFeature().setEnabled(config.isAutoStreamFeatureEnabled());
        }
    }

    private static void loadConfig() {
        if (!Files.exists(configFile())) {
            data = new NyahConfigData();
            data.normalize();
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(configFile())) {
            data = GSON.fromJson(reader, NyahConfigData.class);
        } catch (IOException exception) {
            NiamodClient.LOGGER.error("Error loading config file!", exception);
            data = new NyahConfigData();
        }

        if (data == null) {
            data = new NyahConfigData();
        }
        data.normalize();
    }

    private static Minecraft minecraft() {
        return Minecraft.getInstance();
    }

    private static Path configDir() {
        return minecraft().gameDirectory.toPath().resolve("config");
    }

    private static Path configFile() {
        return configDir().resolve("nyah-mod.json");
    }
}
