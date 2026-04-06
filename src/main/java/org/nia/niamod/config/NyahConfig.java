package org.nia.niamod.config;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;
import org.nia.niamod.NiamodClient;
import org.nia.niamod.config.setting.ColorSetting;
import org.nia.niamod.config.setting.ChoiceSetting;
import org.nia.niamod.config.setting.ConfigSetting;
import org.nia.niamod.config.setting.FloatSetting;
import org.nia.niamod.config.setting.IntSetting;
import org.nia.niamod.config.setting.SettingCategory;
import org.nia.niamod.config.setting.SettingSection;
import org.nia.niamod.config.setting.StringSetting;
import org.nia.niamod.features.IgnoreFeature;
import org.nia.niamod.managers.FeatureManager;
import org.nia.niamod.managers.KeybindManager;
import org.nia.niamod.ui.clickgui.NiaClickGuiScreen;
import org.nia.niamod.ui.clickgui.theme.ClickGuiFontOption;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.nia.niamod.NiamodClient.mc;

public class NyahConfig {
    private static final Path CONFIG_DIR = Paths.get(mc.gameDirectory.getPath(), "config");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("nyah-mod.json");
    private static final Gson GSON = new Gson();
    private static final List<SettingSection> SECTIONS = new ArrayList<>();

    public static NyahConfigData nyahConfigData;

    public static void init() {
        loadConfig();
        registerSections();
        KeybindManager.registerKeybinding("Open NiaMod Click GUI", GLFW.GLFW_KEY_RIGHT_SHIFT, () -> mc.setScreen(getConfigScreen()));
    }

    public static void onFeaturesInitialized() {
        applyFeatureStates();
    }

    public static Screen getConfigScreen() {
        return getConfigScreen(NiamodClient.mc.screen);
    }

    public static Screen getConfigScreen(Screen currentScreen) {
        return new NiaClickGuiScreen(currentScreen);
    }

    public static List<SettingSection> getSections(SettingCategory category) {
        return SECTIONS.stream().filter(section -> section.getCategory() == category).toList();
    }

    private static void registerSections() {
        SECTIONS.clear();

        SECTIONS.add(SettingSection.standard(
                "client",
                "Client",
                "Core Wynncraft data and shared client preferences.",
                SettingCategory.GENERAL,
                null,
                null,
                List.of(
                        string("api_base", "API URL", "Base Wynncraft API URL.", nyahConfigData::getApiBase, nyahConfigData::setApiBase),
                        string("guild_name", "Guild Name", "Guild used by social tools like Ignore.", nyahConfigData::getGuildName, NyahConfig::setGuildName)
                )
        ));

        SECTIONS.add(SettingSection.standard(
                "chat_encryption",
                "Chat Encryption",
                "Encrypt and decode guild messages with a shared key.",
                SettingCategory.GENERAL,
                nyahConfigData::isChatEncryptionFeatureEnabled,
                value -> {
                    nyahConfigData.setChatEncryptionFeatureEnabled(value);
                    applyFeatureStates();
                    save();
                },
                List.of(
                        bool("encryption_enabled", "Encrypt Messages", "Master toggle for chat encryption behavior.", nyahConfigData::isEncryptionEnabled, nyahConfigData::setEncryptionEnabled),
                        string("encryption_prefix", "Encryption Prefix", "Messages starting with this prefix are encrypted.", nyahConfigData::getEncryptionPrefix, nyahConfigData::setEncryptionPrefix),
                        string("encryption_key", "Encryption Key", "Shared AES key material.", nyahConfigData::getEncryptionKey, nyahConfigData::setEncryptionKey),
                        integer("salt_length", "Salt Length", "Initialization vector length in bytes.", 0, 64, nyahConfigData::getSaltLength, nyahConfigData::setSaltLength)
                )
        ));

        SECTIONS.add(SettingSection.standard(
                "view_model",
                "View Model",
                "Hand offsets, rotations and held-item bobbing.",
                SettingCategory.GENERAL,
                null,
                null,
                List.of(
                        integer("x_offset", "X Offset", "Horizontal offset in hundredths.", -150, 150, nyahConfigData::getXOffset, nyahConfigData::setXOffset),
                        integer("y_offset", "Y Offset", "Vertical offset in hundredths.", -150, 150, nyahConfigData::getYOffset, nyahConfigData::setYOffset),
                        integer("z_offset", "Z Offset", "Depth offset in hundredths.", -150, 50, nyahConfigData::getZOffset, nyahConfigData::setZOffset),
                        integer("x_rotation", "X Rotation", "Pitch rotation.", -180, 180, nyahConfigData::getXRotation, nyahConfigData::setXRotation),
                        integer("y_rotation", "Y Rotation", "Yaw rotation.", -180, 180, nyahConfigData::getYRotation, nyahConfigData::setYRotation),
                        integer("z_rotation", "Z Rotation", "Roll rotation.", -180, 180, nyahConfigData::getZRotation, nyahConfigData::setZRotation),
                        floating("item_scale", "Item Scale", "Held item scale multiplier.", 0.1f, 3.0f, nyahConfigData::getItemScale, nyahConfigData::setItemScale),
                        bool("disable_held_bobbing", "Disable Bobbing", "Renders hands without vanilla bobbing.", nyahConfigData::isDisableHeldBobbing, nyahConfigData::setDisableHeldBobbing)
                )
        ));

        SECTIONS.add(SettingSection.standard(
                "resource_tick",
                "Resource Tick",
                "Tracks the current guild resource tick from territory data.",
                SettingCategory.WAR,
                nyahConfigData::isResourceTickFeatureEnabled,
                value -> {
                    nyahConfigData.setResourceTickFeatureEnabled(value);
                    applyFeatureStates();
                    save();
                },
                List.of()
        ));

        SECTIONS.add(SettingSection.standard(
                "territory_boxes",
                "Territory Boxes",
                "Draws upcoming attack territories directly in-world.",
                SettingCategory.WAR,
                nyahConfigData::isWarTimersFeatureEnabled,
                value -> {
                    nyahConfigData.setWarTimersFeatureEnabled(value);
                    applyFeatureStates();
                    save();
                },
                List.of(
                        color("territory_color", "Box Color", "Color used for queued territory outlines.", nyahConfigData::getColor, nyahConfigData::setColor),
                        integer("maximum_territories", "Max Territories", "Maximum queued territories to render.", 1, 30, nyahConfigData::getMaximumTerritories, nyahConfigData::setMaximumTerritories),
                        integer("maximum_distance", "Max Distance", "Max squared-distance filter input.", 50, 5000, nyahConfigData::getMaximumDistance, nyahConfigData::setMaximumDistance)
                )
        ));

        SECTIONS.add(SettingSection.standard(
                "consumable_labels",
                "Consumable Labels",
                "Compact crafted-consumable identifiers in inventories.",
                SettingCategory.WAR,
                nyahConfigData::isConsuTextFeatureEnabled,
                value -> {
                    nyahConfigData.setConsuTextFeatureEnabled(value);
                    applyFeatureStates();
                    save();
                },
                List.of(
                        floating("label_scale", "Label Scale", "Overlay text scale.", 0.25f, 2.5f, nyahConfigData::getIdScale, nyahConfigData::setIdScale),
                        integer("label_x_offset", "Label X Offset", "Horizontal label offset.", -16, 16, nyahConfigData::getIdXOffset, nyahConfigData::setIdXOffset),
                        integer("label_y_offset", "Label Y Offset", "Vertical label offset.", -16, 16, nyahConfigData::getIdYOffset, nyahConfigData::setIdYOffset)
                )
        ));

        SECTIONS.add(SettingSection.standard(
                "tower_ehp",
                "Tower EHP",
                "Replaces tower boss-bar HP with effective HP.",
                SettingCategory.WAR,
                nyahConfigData::isWarTowerEhpFeatureEnabled,
                value -> {
                    nyahConfigData.setWarTowerEhpFeatureEnabled(value);
                    applyFeatureStates();
                    save();
                },
                List.of(
                        bool("replace_tower_hp", "Replace Tower HP", "Swap tower HP text for effective HP.", nyahConfigData::isReplaceTowerHP, nyahConfigData::setReplaceTowerHP)
                )
        ));

        SECTIONS.add(SettingSection.ignoreManager(
                "ignore_manager",
                "Ignore Manager",
                "Guild-member favorites, avoids and bulk ignore tools.",
                SettingCategory.SOCIAL,
                nyahConfigData::isIgnoreFeatureEnabled,
                value -> {
                    nyahConfigData.setIgnoreFeatureEnabled(value);
                    applyFeatureStates();
                    save();
                },
                FeatureManager::getIgnoreFeature
        ));

        SECTIONS.add(SettingSection.standard(
                "shout_filter",
                "Shout Filter",
                "Reserved module slot for chat filtering logic.",
                SettingCategory.SOCIAL,
                nyahConfigData::isShoutFilterFeatureEnabled,
                value -> {
                    nyahConfigData.setShoutFilterFeatureEnabled(value);
                    applyFeatureStates();
                    save();
                },
                List.of()
        ));
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE.toFile())) {
            GSON.toJson(nyahConfigData, writer);
        } catch (IOException exception) {
            NiamodClient.LOGGER.error("Failed to save config", exception);
        }
    }

    public static void applyFeatureStates() {
        if (FeatureManager.getResTickFeature() != null) FeatureManager.getResTickFeature().setEnabled(nyahConfigData.isResourceTickFeatureEnabled());
        if (FeatureManager.getChatEncryptionFeature() != null) FeatureManager.getChatEncryptionFeature().setEnabled(nyahConfigData.isChatEncryptionFeatureEnabled());
        if (FeatureManager.getWarTimersFeature() != null) FeatureManager.getWarTimersFeature().setEnabled(nyahConfigData.isWarTimersFeatureEnabled());
        if (FeatureManager.getIgnoreFeature() != null) {
            FeatureManager.getIgnoreFeature().setEnabled(nyahConfigData.isIgnoreFeatureEnabled());
            if (FeatureManager.getIgnoreFeature().isEnabled()) {
                FeatureManager.getIgnoreFeature().syncGuildMembers();
            }
        }
        if (FeatureManager.getWarTowerEHPFeature() != null) FeatureManager.getWarTowerEHPFeature().setEnabled(nyahConfigData.isWarTowerEhpFeatureEnabled());
        if (FeatureManager.getConsuTextFeature() != null) FeatureManager.getConsuTextFeature().setEnabled(nyahConfigData.isConsuTextFeatureEnabled());
        if (FeatureManager.getShoutFilterFeature() != null) FeatureManager.getShoutFilterFeature().setEnabled(nyahConfigData.isShoutFilterFeatureEnabled());
    }

    private static void loadConfig() {
        try {
            if (!Files.exists(CONFIG_FILE)) {
                Files.createDirectories(CONFIG_DIR);
                Files.createFile(CONFIG_FILE);
                nyahConfigData = new NyahConfigData();
                save();
                return;
            }

            nyahConfigData = GSON.fromJson(new FileReader(CONFIG_FILE.toFile()), NyahConfigData.class);
            if (nyahConfigData == null) {
                nyahConfigData = new NyahConfigData();
            }
            if (nyahConfigData.getFavouritePlayers() == null) nyahConfigData.setFavouritePlayers(new ArrayList<>());
            if (nyahConfigData.getAvoidedPlayers() == null) nyahConfigData.setAvoidedPlayers(new ArrayList<>());
            nyahConfigData.setClickGuiFont(ClickGuiFontOption.resolve(nyahConfigData.getClickGuiFont()).getKey());
        } catch (IOException exception) {
            NiamodClient.LOGGER.error("Error loading config file!", exception);
            nyahConfigData = new NyahConfigData();
        }
    }

    private static StringSetting string(String id, String title, String description, ConfigGetter<String> getter, ConfigSetter<String> setter) {
        return new StringSetting(id, title, description, getter::get, value -> {
            setter.set(value);
            save();
        });
    }

    private static IntSetting integer(String id, String title, String description, int min, int max, ConfigGetter<Integer> getter, ConfigSetter<Integer> setter) {
        return new IntSetting(id, title, description, min, max, getter::get, value -> {
            setter.set(value);
            save();
        });
    }

    private static FloatSetting floating(String id, String title, String description, float min, float max, ConfigGetter<Float> getter, ConfigSetter<Float> setter) {
        return new FloatSetting(id, title, description, min, max, getter::get, value -> {
            setter.set(value);
            save();
        });
    }

    private static org.nia.niamod.config.setting.BooleanSetting bool(String id, String title, String description, ConfigGetter<Boolean> getter, ConfigSetter<Boolean> setter) {
        return new org.nia.niamod.config.setting.BooleanSetting(id, title, description, getter::get, value -> {
            setter.set(value);
            save();
        });
    }

    private static ColorSetting color(String id, String title, String description, ConfigGetter<Integer> getter, ConfigSetter<Integer> setter) {
        return new ColorSetting(id, title, description, getter::get, value -> {
            setter.set(value);
            save();
        });
    }

    private static ChoiceSetting choice(
            String id,
            String title,
            String description,
            ConfigGetter<String> getter,
            ConfigSetter<String> setter,
            List<String> options,
            java.util.function.Function<String, String> labelResolver
    ) {
        return new ChoiceSetting(id, title, description, getter::get, value -> {
            setter.set(value);
            save();
        }, options, labelResolver);
    }

    private static void setGuildName(String guildName) {
        nyahConfigData.setGuildName(guildName);
        save();

        if (FeatureManager.getIgnoreFeature() != null && FeatureManager.getIgnoreFeature().isEnabled()) {
            FeatureManager.getIgnoreFeature().syncGuildMembers();
        }
    }

    private static void setClickGuiFont(String fontId) {
        nyahConfigData.setClickGuiFont(ClickGuiFontOption.resolve(fontId).getKey());
        save();
    }

    @FunctionalInterface
    private interface ConfigGetter<T> {
        T get();
    }

    @FunctionalInterface
    private interface ConfigSetter<T> {
        void set(T value);
    }

    @Getter
    @Setter
    public static class NyahConfigData {
        private String apiBase = "https://api.wynncraft.com/v3/";
        private String guildName = "Nerfuria";
        private String clickGuiFont = "minecraft_uniform";

        private boolean resourceTickFeatureEnabled = true;
        private boolean chatEncryptionFeatureEnabled = true;
        private boolean warTimersFeatureEnabled = true;
        private boolean ignoreFeatureEnabled = true;
        private boolean warTowerEhpFeatureEnabled = true;
        private boolean consuTextFeatureEnabled = true;
        private boolean shoutFilterFeatureEnabled = true;

        private boolean encryptionEnabled = true;
        private String encryptionPrefix = "@";
        private String encryptionKey = "six seven";
        private int saltLength = 16;

        private int color = 0xFFFFFF;
        private int maximumDistance = 1000;
        private int maximumTerritories = 10;

        private float idScale = 0.7f;
        private int idXOffset = 1;
        private int idYOffset = 1;

        private int xOffset = 0;
        private int yOffset = 0;
        private int zOffset = 0;
        private int xRotation = 0;
        private int yRotation = 0;
        private int zRotation = 0;
        private float itemScale = 1.0f;
        private boolean disableHeldBobbing = true;

        private boolean replaceTowerHP = true;
        private List<String> favouritePlayers = new ArrayList<>();
        private List<String> avoidedPlayers = new ArrayList<>();
    }
}
