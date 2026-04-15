package org.nia.niamod.config;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;
import org.nia.niamod.NiamodClient;
import org.nia.niamod.config.setting.BooleanSetting;
import org.nia.niamod.config.setting.ButtonSetting;
import org.nia.niamod.config.setting.ChoiceSetting;
import org.nia.niamod.config.setting.ColorSetting;
import org.nia.niamod.config.setting.FloatSetting;
import org.nia.niamod.config.setting.IntSetting;
import org.nia.niamod.config.setting.SettingSection;
import org.nia.niamod.config.setting.StringSetting;
import org.nia.niamod.managers.FeatureManager;
import org.nia.niamod.managers.KeybindManager;
import org.nia.niamod.models.config.ClickGuiAnimationMode;
import org.nia.niamod.models.config.RadianceOverlayMode;
import org.nia.niamod.models.config.SettingCategory;
import org.nia.niamod.models.config.ShoutReplacement;
import org.nia.niamod.models.gui.NiaClickGuiScreen;
import org.nia.niamod.models.gui.OverlayManagerScreen;
import org.nia.niamod.models.gui.theme.ClickGuiFontOption;
import org.nia.niamod.models.gui.theme.ClickGuiThemeOption;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

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
        return SECTIONS.stream().filter(section -> section.category() == category).toList();
    }

    private static void registerSections() {
        SECTIONS.clear();

        SECTIONS.add(SettingSection.standard(
                "client",
                "Client",
                "Core Wynncraft data and shared client preferences.",
                SettingCategory.GENERAL,
                () -> true,
                (a) -> {
                },
                List.of(
                        string("api_base", "API URL", "Base Wynncraft API URL.", () -> nyahConfigData.getApiBase(), val -> nyahConfigData.setApiBase(val)),
                        string("guild_name", "Guild Name", "Guild used by social tools like Ignore.", () -> nyahConfigData.getGuildName(), NyahConfig::setGuildName)
                )
        ));

        SECTIONS.add(SettingSection.standard(
                "gui",
                "GUI Settings",
                "Visual customizations for the Click GUI.",
                SettingCategory.GENERAL,
                () -> true,
                (a) -> {
                },
                List.of(
                        choice("click_gui_theme", "GUI Theme", "Color scheme for the Click GUI.", NyahConfig::getClickGuiThemeEnum, NyahConfig::setClickGuiThemeEnum, ClickGuiThemeOption.class),
                        choice("click_gui_font", "GUI Font", "Font used in the Click GUI.", NyahConfig::getClickGuiFontEnum, NyahConfig::setClickGuiFontEnum, ClickGuiFontOption.class),
                        choice("click_gui_animation", "GUI Animation", "Opening and closing effect for the Click GUI.",
                                nyahConfigData::getClickGuiAnimation, nyahConfigData::setClickGuiAnimation, ClickGuiAnimationMode.class),
                        integer("animation_time", "Animation Time", "Time for config animation.", 100, 2000, () -> nyahConfigData.getAnimationTime(), val -> nyahConfigData.setAnimationTime(val)),
                        floating("gui_opacity", "GUI Opacity", "Overall background transparency.", 0.1f, 1.0f, () -> nyahConfigData.getGuiOpacity(), val -> nyahConfigData.setGuiOpacity(val)),
                        color("custom_gui_background", "Custom Background", "Background color for the Custom theme.", NyahConfig::getActiveThemeBackground, val -> {
                            nyahConfigData.setCustomGuiBackground(val);
                            if (val != getActiveThemeBackground())
                                nyahConfigData.setClickGuiTheme(ClickGuiThemeOption.CUSTOM.name());
                            save();
                        }),
                        color("custom_gui_secondary", "Custom Secondary", "Secondary color for the Custom theme.", NyahConfig::getActiveThemeSecondary, val -> {
                            nyahConfigData.setCustomGuiSecondary(val);
                            if (val != getActiveThemeSecondary())
                                nyahConfigData.setClickGuiTheme(ClickGuiThemeOption.CUSTOM.name());
                            save();
                        }),
                        color("custom_gui_accent", "Custom Accent", "Accent color for the Custom theme.", NyahConfig::getActiveThemeAccent, val -> {
                            nyahConfigData.setCustomGuiAccent(val);
                            if (val != getActiveThemeAccent())
                                nyahConfigData.setClickGuiTheme(ClickGuiThemeOption.CUSTOM.name());
                            save();
                        })
                )
        ));

        SECTIONS.add(SettingSection.standard(
                "chat_encryption",
                "Chat Encryption",
                "Encrypt and decode guild messages with a shared key.",
                SettingCategory.SOCIAL,
                () -> true,
                value -> {
                },
                List.of(
                        bool("enabled", "Enabled", "Enable this feature.", () -> nyahConfigData.isChatEncryptionFeatureEnabled(), val -> {
                            nyahConfigData.setChatEncryptionFeatureEnabled(val);
                            applyFeatureStates();
                        }),
                        string("encryption_prefix", "Encryption Prefix", "Messages starting with this prefix are encrypted.", () -> nyahConfigData.getEncryptionPrefix(), val -> nyahConfigData.setEncryptionPrefix(val)),
                        string("encryption_key", "Encryption Key", "Shared AES key material.", () -> nyahConfigData.getEncryptionKey(), val -> nyahConfigData.setEncryptionKey(val)),
                        integer("salt_length", "Salt Length", "Initialization vector length in bytes.", 0, 64, () -> nyahConfigData.getSaltLength(), val -> nyahConfigData.setSaltLength(val))
                )
        ));

        SECTIONS.add(SettingSection.standard(
                "auto_stream",
                "Auto Stream",
                "Automatically stream when you changed worlds",
                SettingCategory.SOCIAL,
                () -> true,
                value -> {
                },
                List.of(
                        bool("enabled", "Enabled", "Enable this feature.", () -> nyahConfigData.isAutoStreamFeatureEnabled(), val -> {
                            nyahConfigData.setAutoStreamFeatureEnabled(val);
                            applyFeatureStates();
                        }),
                        integer("timeout", "Timeout", "Time to wait between stream boss bars before attempting to re-stream", 100, 10000, () -> nyahConfigData.getStreamCooldown(), val -> nyahConfigData.setStreamCooldown(val))
                )
        ));

        SECTIONS.add(SettingSection.standard(
                "view_model",
                "View Model",
                "Hand offsets, rotations",
                SettingCategory.GENERAL,
                () -> true,
                value -> {
                },
                List.of(
                        bool("enabled", "Enabled", "Enable this feature.", () -> nyahConfigData.isViewModelFeatureEnabled(), val -> {
                            nyahConfigData.setViewModelFeatureEnabled(val);
                            applyFeatureStates();
                        }),
                        integer("x_offset", "X Offset", "Horizontal offset in hundredths.", -150, 150, () -> nyahConfigData.getXOffset(), val -> nyahConfigData.setXOffset(val)),
                        integer("y_offset", "Y Offset", "Vertical offset in hundredths.", -150, 150, () -> nyahConfigData.getYOffset(), val -> nyahConfigData.setYOffset(val)),
                        integer("z_offset", "Z Offset", "Depth offset in hundredths.", -150, 50, () -> nyahConfigData.getZOffset(), val -> nyahConfigData.setZOffset(val)),
                        integer("x_rotation", "X Rotation", "Pitch rotation.", -180, 180, () -> nyahConfigData.getXRotation(), val -> nyahConfigData.setXRotation(val)),
                        integer("y_rotation", "Y Rotation", "Yaw rotation.", -180, 180, () -> nyahConfigData.getYRotation(), val -> nyahConfigData.setYRotation(val)),
                        integer("z_rotation", "Z Rotation", "Roll rotation.", -180, 180, () -> nyahConfigData.getZRotation(), val -> nyahConfigData.setZRotation(val)),
                        floating("item_scale", "Item Scale", "Held item scale multiplier.", 0.1f, 3.0f, () -> nyahConfigData.getItemScale(), val -> nyahConfigData.setItemScale(val)),
                        bool("disable_held_bobbing", "Disable Bobbing", "Renders hands without vanilla bobbing.", () -> nyahConfigData.isDisableHeldBobbing(), val -> nyahConfigData.setDisableHeldBobbing(val))
                )
        ));

        SECTIONS.add(SettingSection.standard(
                "resource_tick",
                "Resource Tick",
                "Tracks the current guild resource tick from territory data.",
                SettingCategory.WAR,
                () -> true,
                value -> {
                },
                List.of(
                        bool("enabled", "Enabled", "Enable this feature.", () -> nyahConfigData.isResourceTickFeatureEnabled(), val -> {
                            nyahConfigData.setResourceTickFeatureEnabled(val);
                            applyFeatureStates();
                        }),
                        button("res_overlay", "Move Overlay", "Move the resource tick overlay", () -> mc.setScreen(new OverlayManagerScreen(mc.screen, List.of(FeatureManager.getResTickFeature().getResTickOverlay()))), "Open Editor")
                )
        ));

        SECTIONS.add(SettingSection.standard(
                "territory_boxes",
                "Territory Boxes",
                "Draws upcoming attack territories directly in-world.",
                SettingCategory.WAR,
                () -> true,
                value -> {
                },
                List.of(
                        bool("enabled", "Enabled", "Enable this feature.", () -> nyahConfigData.isWarTimersFeatureEnabled(), val -> {
                            nyahConfigData.setWarTimersFeatureEnabled(val);
                            applyFeatureStates();
                        }),
                        color("territory_color", "Box Color", "Color used for queued territory outlines.", () -> nyahConfigData.getColor(), val -> nyahConfigData.setColor(val)),
                        color("territory_color_inside", "Inside Box Color", "Color used for queued territory outlines that you are inside.", () -> nyahConfigData.getColorInside(), val -> nyahConfigData.setColorInside(val)),
                        color("unqueued_territory_color", "Unqueued Box Color", "Color used for unqueued territory outlines.", () -> nyahConfigData.getNotQColor(), val -> nyahConfigData.setNotQColor(val)),
                        color("unqueued_territory_inside_color", "Inside Unqueued Box Color", "Color used for unqueued territory outlines that you are inside.", () -> nyahConfigData.getNotQInsideColor(), val -> nyahConfigData.setNotQInsideColor(val)),
                        integer("maximum_territories", "Max Territories", "Maximum queued territories to render.", 1, 30, () -> nyahConfigData.getMaximumTerritories(), val -> nyahConfigData.setMaximumTerritories(val)),
                        integer("maximum_distance", "Max Distance", "Max squared-distance filter input.", 50, 5000, () -> nyahConfigData.getMaximumDistance(), val -> nyahConfigData.setMaximumDistance(val)),
                        integer("maximum_terr_warn", "Max Territory Warning", "How many upcoming attack timers to warn you about (set 0 to disable)", 0, 10, () -> nyahConfigData.getTerritoryWarningCount(), val -> nyahConfigData.setTerritoryWarningCount(val)),
                        integer("max_time_warn", "Max Queue Time", "Maximum time of an attack timer to be included (min)", 1, 10, () -> nyahConfigData.getMaxTimeTerr(), val -> nyahConfigData.setMaxTimeTerr(val)),
                        integer("often_warn", "Warn Speed", "How many seconds between warns", 10, 120, () -> nyahConfigData.getWarnTime(), val -> nyahConfigData.setWarnTime(val))
                )
        ));

        SECTIONS.add(SettingSection.standard(
                "consumable_labels",
                "Consumable Labels",
                "Compact crafted-consumable identifiers in inventories.",
                SettingCategory.WAR,
                () -> true,
                value -> {
                },
                List.of(
                        bool("enabled", "Enabled", "Enable this feature.", () -> nyahConfigData.isConsuTextFeatureEnabled(), val -> {
                            nyahConfigData.setConsuTextFeatureEnabled(val);
                            applyFeatureStates();
                        }),
                        floating("label_scale", "Label Scale", "Overlay text scale.", 0.25f, 2.5f, () -> nyahConfigData.getIdScale(), val -> nyahConfigData.setIdScale(val)),
                        integer("label_x_offset", "Label X Offset", "Horizontal label offset.", -16, 16, () -> nyahConfigData.getIdXOffset(), val -> nyahConfigData.setIdXOffset(val)),
                        integer("label_y_offset", "Label Y Offset", "Vertical label offset.", -16, 16, () -> nyahConfigData.getIdYOffset(), val -> nyahConfigData.setIdYOffset(val))
                )
        ));

        SECTIONS.add(SettingSection.standard(
                "tower_ehp",
                "Tower EHP",
                "Replaces tower boss-bar HP with effective HP.",
                SettingCategory.WAR,
                () -> true,
                value -> {
                },
                List.of(
                        bool("enabled", "Enabled", "Enable this feature.", () -> nyahConfigData.isWarTowerEhpFeatureEnabled(), val -> {
                            nyahConfigData.setWarTowerEhpFeatureEnabled(val);
                            applyFeatureStates();
                        })
                )
        ));

        SECTIONS.add(SettingSection.standard(
                "shout_filter",
                "Shout Filter",
                "Modify shouts to be less obstructive.",
                SettingCategory.SOCIAL,
                () -> true,
                value -> {
                },
                List.of(
                        bool("enabled", "Enabled", "Enable this feature.", () -> nyahConfigData.isShoutFilterFeatureEnabled(), val -> {
                            nyahConfigData.setShoutFilterFeatureEnabled(val);
                            applyFeatureStates();
                        }),
                        choice("filter_mode", "Shout Filter Mode", "What to replace shouts with", () -> nyahConfigData.getShoutFilterMode(), val -> nyahConfigData.setShoutFilterMode(val), ShoutReplacement.class)
                )
        ));

        SECTIONS.add(SettingSection.standard(
                "radiance_sync",
                "Radiance Sync",
                "Tracks Radiance with Guard Partners in War",
                SettingCategory.WAR,
                () -> true,
                value -> {
                },
                List.of(
                        bool("enabled", "Enabled", "Enable this feature.", nyahConfigData::isRadianceSyncEnabled, val -> {
                            nyahConfigData.setRadianceSyncEnabled(val);
                            applyFeatureStates();
                        }),
                        string("group_key", "Group Key", "Shared sync group key (shared with party members).", nyahConfigData::getRadianceSyncGroupKey, nyahConfigData::setRadianceSyncGroupKey),
                        integer("self_aspect_tier", "Self Aspect Tier", "Your Radiance aspect tier (0 = none, 1-3).", 0, 3, nyahConfigData::getRadianceSyncSelfTier, nyahConfigData::setRadianceSyncSelfTier),
                        bool("require_war", "Require War", "Only show overlay while in war.", nyahConfigData::isRadianceSyncRequireWar, nyahConfigData::setRadianceSyncRequireWar),
                        choice("overlay_mode", "Overlay Mode", "cast = cast prompt mode; status = radiance remaining.",
                                nyahConfigData::getRadianceSyncOverlayMode, nyahConfigData::setRadianceSyncOverlayMode,
                                RadianceOverlayMode.class),
                        string("worker_url", "Worker URL", "WebSocket worker URL for Radiance sync.",
                                nyahConfigData::getRadianceSyncWorkerUrl, nyahConfigData::setRadianceSyncWorkerUrl),
                        floating("cast_prompt_seconds", "Cast Prompt Seconds", "How long the cast prompt stays visible.", 0.5f, 10.0f,
                                nyahConfigData::getRadianceSyncCastPromptSeconds, nyahConfigData::setRadianceSyncCastPromptSeconds),
                        bool("ping_sound", "Ping Sound", "Play a sound when Radiance is ready.", nyahConfigData::isRadianceSyncPingEnabled, nyahConfigData::setRadianceSyncPingEnabled),
                        floating("ping_volume", "Ping Volume", "Volume of the ping sound.", 0.1f, 2.0f, nyahConfigData::getRadianceSyncPingVolume, nyahConfigData::setRadianceSyncPingVolume),
                        floating("ping_pitch", "Ping Pitch", "Pitch of the ping sound.", 0.5f, 2.0f, nyahConfigData::getRadianceSyncPingPitch, nyahConfigData::setRadianceSyncPingPitch),
                        button("move_overlay", "Move Overlay", "Move the sync overlay", () -> mc.setScreen(new OverlayManagerScreen(mc.screen, List.of(FeatureManager.getRadianceSyncFeature().getOverlay()))), "Open Editor")
                )
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
        if (FeatureManager.getResTickFeature() != null)
            FeatureManager.getResTickFeature().setEnabled(nyahConfigData.isResourceTickFeatureEnabled());
        if (FeatureManager.getChatEncryptionFeature() != null)
            FeatureManager.getChatEncryptionFeature().setEnabled(nyahConfigData.isChatEncryptionFeatureEnabled());
        if (FeatureManager.getWarTimersFeature() != null)
            FeatureManager.getWarTimersFeature().setEnabled(nyahConfigData.isWarTimersFeatureEnabled());
        if (FeatureManager.getWarTowerEHPFeature() != null)
            FeatureManager.getWarTowerEHPFeature().setEnabled(nyahConfigData.isWarTowerEhpFeatureEnabled());
        if (FeatureManager.getConsuTextFeature() != null)
            FeatureManager.getConsuTextFeature().setEnabled(nyahConfigData.isConsuTextFeatureEnabled());
        if (FeatureManager.getShoutFilterFeature() != null)
            FeatureManager.getShoutFilterFeature().setEnabled(nyahConfigData.isShoutFilterFeatureEnabled());
        if (FeatureManager.getViewModelTransformationFeature() != null)
            FeatureManager.getViewModelTransformationFeature().setEnabled(nyahConfigData.isViewModelFeatureEnabled());
        if (FeatureManager.getRadianceSyncFeature() != null)
            FeatureManager.getRadianceSyncFeature().setEnabled(nyahConfigData.isRadianceSyncEnabled());
        if (FeatureManager.getAutoStreamFeature() != null)
            FeatureManager.getAutoStreamFeature().setEnabled(nyahConfigData.isAutoStreamFeatureEnabled());
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
            nyahConfigData.setClickGuiFont(ClickGuiFontOption.resolve(nyahConfigData.getClickGuiFont()).name());
            nyahConfigData.setClickGuiTheme(ClickGuiThemeOption.resolve(nyahConfigData.getClickGuiTheme()).name());
            if (nyahConfigData.getClickGuiAnimation() == null)
                nyahConfigData.setClickGuiAnimation(ClickGuiAnimationMode.NONE);
            nyahConfigData.setClickGuiFont(ClickGuiFontOption.resolve(nyahConfigData.getClickGuiFont()).getKey());
            if (nyahConfigData.getRadianceSyncGroupKey() == null) nyahConfigData.setRadianceSyncGroupKey("");
            if (nyahConfigData.getRadianceSyncOverlayMode() == null)
                nyahConfigData.setRadianceSyncOverlayMode(RadianceOverlayMode.CAST);
            if (nyahConfigData.getRadianceSyncWorkerUrl() == null)
                nyahConfigData.setRadianceSyncWorkerUrl("https://radiancesync.wavelink.workers.dev");
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

    private static ButtonSetting button(String id, String title, String description, Runnable action, String buttonText) {
        return new ButtonSetting(id, title, description, () -> {
            action.run();
            save();
        }, buttonText);
    }

    private static FloatSetting floating(String id, String title, String description, float min, float max, ConfigGetter<Float> getter, ConfigSetter<Float> setter) {
        return new FloatSetting(id, title, description, min, max, getter::get, value -> {
            setter.set(value);
            save();
        });
    }

    private static BooleanSetting bool(String id, String title, String description, ConfigGetter<Boolean> getter, ConfigSetter<Boolean> setter) {
        return new BooleanSetting(id, title, description, getter::get, value -> {
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

    private static <T extends Enum<T>> ChoiceSetting choice(
            String id,
            String title,
            String description,
            ConfigGetter<T> getter,
            ConfigSetter<T> setter,
            Class<T> type
    ) {
        Function<String, String> labelResolver = raw -> {
            try {
                if (type == ClickGuiThemeOption.class) return ClickGuiThemeOption.valueOf(raw).getLabel();
                if (type == ClickGuiFontOption.class) return ClickGuiFontOption.valueOf(raw).getLabel();
            } catch (Exception ignored) {
            }
            String lower = raw.toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
            return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
        };
        return new ChoiceSetting(id, title, description, () -> getter.get().name(), value -> {
            setter.set(T.valueOf(type, value));
            save();
        }, Arrays.stream(type.getEnumConstants()).map(Enum::name).toList(), labelResolver);
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
    }

    private static ClickGuiThemeOption getClickGuiThemeEnum() {
        try {
            return ClickGuiThemeOption.valueOf(nyahConfigData.getClickGuiTheme());
        } catch (Exception e) {
            return ClickGuiThemeOption.DEFAULT;
        }
    }

    private static void setClickGuiThemeEnum(ClickGuiThemeOption theme) {
        nyahConfigData.setClickGuiTheme(theme.name());
        save();
    }

    private static ClickGuiFontOption getClickGuiFontEnum() {
        try {
            return ClickGuiFontOption.valueOf(nyahConfigData.getClickGuiFont());
        } catch (Exception e) {
            return ClickGuiFontOption.JETBRAINS_MONO;
        }
    }

    private static void setClickGuiFontEnum(ClickGuiFontOption font) {
        nyahConfigData.setClickGuiFont(font.name());
        save();
    }

    private static int getActiveThemeBackground() {
        if (getClickGuiThemeEnum() != ClickGuiThemeOption.CUSTOM)
            return getClickGuiThemeEnum().getTheme().getBackground() & 0xFFFFFF;
        return nyahConfigData.getCustomGuiBackground() & 0xFFFFFF;
    }

    private static int getActiveThemeSecondary() {
        if (getClickGuiThemeEnum() != ClickGuiThemeOption.CUSTOM)
            return getClickGuiThemeEnum().getTheme().getSecondary() & 0xFFFFFF;
        return nyahConfigData.getCustomGuiSecondary() & 0xFFFFFF;
    }

    private static int getActiveThemeAccent() {
        if (getClickGuiThemeEnum() != ClickGuiThemeOption.CUSTOM)
            return getClickGuiThemeEnum().getTheme().getAccentColor() & 0xFFFFFF;
        return nyahConfigData.getCustomGuiAccent() & 0xFFFFFF;
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
        private String clickGuiTheme = "DEFAULT";
        private String clickGuiFont = "MINECRAFT_DEFAULT";
        private ClickGuiAnimationMode clickGuiAnimation = ClickGuiAnimationMode.NONE;
        private int animationTime = 1000;
        private float guiOpacity = 0.9f;

        private int customGuiBackground = 0x171A21;
        private int customGuiSecondary = 0x11141B;
        private int customGuiAccent = 0x4794FD;

        private int guiWidth = 500;
        private int guiHeight = 350;

        private boolean shoutReplacementFeatureEnabled = true;
        private boolean resourceTickFeatureEnabled = true;
        private boolean chatEncryptionFeatureEnabled = true;
        private boolean autoStreamFeatureEnabled = true;
        private boolean warTimersFeatureEnabled = true;
        private boolean warTowerEhpFeatureEnabled = true;
        private boolean consuTextFeatureEnabled = true;
        private boolean shoutFilterFeatureEnabled = true;
        private boolean isViewModelFeatureEnabled = true;
        private boolean radianceSyncEnabled = true;


        private ShoutReplacement shoutFilterMode = ShoutReplacement.COLLAPSE;

        private String encryptionPrefix = "@";
        private String encryptionKey = "six seven";
        private int saltLength = 16;

        private int color = 0xFFFFFF;
        private int colorInside = 0xFFFFFF;
        private int notQColor = 0xFFFFFF;
        private int notQInsideColor = 0xFFFFFF;
        private int maximumDistance = 1000;
        private int maximumTerritories = 10;
        private int territoryWarningCount = 0;
        private int maxTimeTerr = 60 * 10;
        private int warnTime = 30;

        private float idScale = 0.7f;
        private int idXOffset = 1;
        private int idYOffset = 1;

        private int streamCooldown = 5000;

        private int xOffset = 0;
        private int yOffset = 0;
        private int zOffset = 0;
        private int xRotation = 0;
        private int yRotation = 0;
        private int zRotation = 0;
        private float itemScale = 1.0f;
        private boolean disableHeldBobbing = true;

        private List<String> favouritePlayers = new ArrayList<>();
        private List<String> avoidedPlayers = new ArrayList<>();


        private boolean radianceSyncRequireWar = true;
        private int radianceSyncSelfTier = 0;
        private float radianceSyncCastPromptSeconds = 2.0f;
        private RadianceOverlayMode radianceSyncOverlayMode = RadianceOverlayMode.CAST;
        private String radianceSyncWorkerUrl = "https://radiancesync.wavelink.workers.dev";
        private boolean radianceSyncPingEnabled = false;
        private float radianceSyncPingVolume = 1.0f;
        private float radianceSyncPingPitch = 1.0f;
        private float radianceSyncOverlayScale = 1.0f;
        private int radianceSyncOverlayOffsetX = 0;
        private int radianceSyncOverlayOffsetY = -10;
        private String radianceSyncGroupKey = "";

        private int resTickOverlayOffsetX = 0;
        private int resTickOverlayOffsetY = 0;
        private float resTickOverlayScale = 1.0f;
    }
}
