package org.nia.niamod.config;

import com.google.gson.Gson;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.nia.niamod.NiamodClient;
import org.nia.niamod.models.gui.SeparatorEntry;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.nia.niamod.NiamodClient.mc;

public class NyahConfig {

    private static final Path CONFIG_DIR = Paths.get(mc.runDirectory.getPath(), "config");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("nyah-mod.json");
    private static final Gson GSON = new Gson();

    private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of("niamod", "config"));
    private static final KeyBinding OPEN_CONFIG = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("Open Config", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY));

    public static NyahConfigData nyahConfigData;

    public static void init() {
        loadConfig();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && OPEN_CONFIG.isPressed()) {
                client.setScreen(getConfigScreen());
            }
        });
    }

    public static Screen getConfigScreen() {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(NiamodClient.mc.currentScreen)
                .setTitle(Text.literal("Nyah Mod"));
        ConfigEntryBuilder eb = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(Text.of("General"));

        general.addEntry(new SeparatorEntry(Text.of("Miscellaneous"), null));
        general.addEntry(eb.startStrField(Text.of("API URL"), nyahConfigData.apiBase)
                .setTooltip(Text.of("Base Wynncraft API URL"))
                .setDefaultValue("https://api.wynncraft.com/v3/")
                .setSaveConsumer(v -> nyahConfigData.apiBase = v)
                .build());
        general.addEntry(eb.startStrField(Text.of("Guild Name"), nyahConfigData.guildName)
                .setDefaultValue("Nerfuria")
                .setSaveConsumer(v -> nyahConfigData.guildName = v)
                .requireRestart()
                .build());

        general.addEntry(new SeparatorEntry(Text.of("Chat Encryption"), null));
        general.addEntry(eb.startBooleanToggle(Text.of("Enable"), nyahConfigData.encryptionEnabled)
                .setDefaultValue(true)
                .setSaveConsumer(v -> nyahConfigData.encryptionEnabled = v)
                .build());
        general.addEntry(eb.startStrField(Text.of("Encryption Prefix"), nyahConfigData.encryptionPrefix)
                .setTooltip(Text.of("What messages to be encrypted should be prefaced with"))
                .setDefaultValue("@")
                .setSaveConsumer(v -> nyahConfigData.encryptionPrefix = v)
                .build());
        general.addEntry(eb.startStrField(Text.of("Encryption Key"), nyahConfigData.encryptionKey)
                .setErrorSupplier(key -> key.isEmpty()
                        ? Optional.of(Text.literal("The encryption key cannot be empty!").withColor(0xFF0000))
                        : Optional.empty())
                .setTooltip(Text.of("Key to use to encrypt your messages"))
                .setDefaultValue("six seven")
                .setSaveConsumer(v -> nyahConfigData.encryptionKey = v)
                .build());
        general.addEntry(eb.startIntField(Text.of("Salt Length"), nyahConfigData.saltLength)
                .setTooltip(Text.of("Salt length in bytes"))
                .setDefaultValue(16)
                .setMin(0)
                .setSaveConsumer(v -> nyahConfigData.saltLength = v)
                .build());

        general.addEntry(new SeparatorEntry(Text.of("Held Item Transformation"), null));
        general.addEntry(eb.startIntField(Text.of("X Offset"), nyahConfigData.xOffset)
                .setDefaultValue(0)
                .setMin(-150)
                .setMax(150)
                .setSaveConsumer(v -> nyahConfigData.xOffset = v)
                .build());
        general.addEntry(eb.startIntField(Text.of("Y Offset"), nyahConfigData.yOffset)
                .setDefaultValue(0)
                .setMin(-150)
                .setMax(150)
                .setSaveConsumer(v -> nyahConfigData.yOffset = v)
                .build());
        general.addEntry(eb.startIntField(Text.of("Z Offset"), nyahConfigData.zOffset)
                .setDefaultValue(0)
                .setMin(-150)
                .setMax(50)
                .setSaveConsumer(v -> nyahConfigData.zOffset = v)
                .build());
        general.addEntry(eb.startIntField(Text.of("X Rotation"), nyahConfigData.xRotation)
                .setDefaultValue(0)
                .setMin(-180)
                .setMax(180)
                .setSaveConsumer(v -> nyahConfigData.xRotation = v)
                .build());
        general.addEntry(eb.startIntField(Text.of("Y Rotation"), nyahConfigData.yRotation)
                .setDefaultValue(0)
                .setMin(-180)
                .setMax(180)
                .setSaveConsumer(v -> nyahConfigData.yRotation = v)
                .build());
        general.addEntry(eb.startIntField(Text.of("Z Rotation"), nyahConfigData.zRotation)
                .setDefaultValue(0)
                .setMin(-180)
                .setMax(180)
                .setSaveConsumer(v -> nyahConfigData.zRotation = v)
                .build());
        general.addEntry(eb.startFloatField(Text.of("Item Scale"), nyahConfigData.itemScale)
                .setDefaultValue(1.0f)
                .setSaveConsumer(v -> nyahConfigData.itemScale = v)
                .build());
        general.addEntry(eb.startBooleanToggle(Text.of("Disable Bobbing"), nyahConfigData.disableHeldBobbing)
                .setDefaultValue(false)
                .setSaveConsumer(v -> nyahConfigData.disableHeldBobbing = v)
                .build());

        ConfigCategory war = builder.getOrCreateCategory(Text.of("War"));
        war.addEntry(new SeparatorEntry(Text.of("Territory Boxes"), null));
        war.addEntry(eb.startColorField(Text.of("Territory Box Colour"), nyahConfigData.color)
                .setTooltip(Text.of("Colour of boxes showing queued territory boundaries"))
                .setDefaultValue(0xFFFFFF)
                .setSaveConsumer(v -> nyahConfigData.color = v)
                .build());
        war.addEntry(eb.startIntField(Text.of("Maximum Territories"), nyahConfigData.maximumTerritories)
                .setTooltip(Text.of("Maximum territory regions to render at once"))
                .setDefaultValue(10)
                .setSaveConsumer(v -> nyahConfigData.maximumTerritories = v)
                .build());
        war.addEntry(eb.startIntField(Text.of("Maximum Distance"), nyahConfigData.maximumDistance)
                .setTooltip(Text.of("Furthest territory box to render at once"))
                .setDefaultValue(1000)
                .setSaveConsumer(v -> nyahConfigData.maximumDistance = v)
                .build());

        builder.setSavingRunnable(nyahConfigData::save);
        return builder.build();
    }


    private static void loadConfig() {
        try {
            if (!Files.exists(CONFIG_FILE)) {
                Files.createDirectories(CONFIG_DIR);
                Files.createFile(CONFIG_FILE);
                nyahConfigData = new NyahConfigData();
                nyahConfigData.save();
                return;
            }
            nyahConfigData = GSON.fromJson(new FileReader(CONFIG_FILE.toFile()), NyahConfigData.class);
        } catch (IOException e) {
            NiamodClient.LOGGER.error("Error loading config file!", e);
            nyahConfigData = new NyahConfigData();
        }
    }

    public static class NyahConfigData {
        public String apiBase = "https://api.wynncraft.com/v3/";
        public String guildName = "Nerfuria";

        public boolean encryptionEnabled = true;
        public String encryptionPrefix = "$";
        public String encryptionKey = "six seven";
        public int saltLength = 16;


        public int color = 0xFFFFFF;
        public int maximumDistance = 1000;
        public int maximumTerritories = 10;

        public int xOffset = 0;
        public int yOffset = 0;
        public int zOffset = 0;
        public int xRotation = 0;
        public int yRotation = 0;
        public int zRotation = 0;
        public float itemScale = 1.0f;
        public boolean disableHeldBobbing = true;

        public void save() {
            try (FileWriter writer = new FileWriter(CONFIG_FILE.toFile())) {
                GSON.toJson(this, writer);
            } catch (IOException e) {
                NiamodClient.LOGGER.error("Failed to save config", e);
            }
        }
    }
}