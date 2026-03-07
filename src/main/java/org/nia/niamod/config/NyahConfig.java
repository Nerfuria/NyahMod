package org.nia.niamod.config;

import com.google.gson.Gson;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.nia.niamod.NiamodClient;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;


public class NyahConfig {

    private static final Path config_dir = Paths.get(MinecraftClient.getInstance().runDirectory.getPath() + "/config");
    private static final Path config_file = Paths.get(config_dir + "/nyah-mod.json");
    private static final KeyBinding.Category niamodConfig = KeyBinding.Category.create(Identifier.of("niamod", "config"));
    private static final KeyBinding openConfig = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.niamod.open_config", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, niamodConfig));
    public static NyahConfigData nyahConfigData;

    public static void init() {
        getConfigData();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            if (openConfig.isPressed()) {
                client.setScreen(getConfigScreen());
            }
        });
    }

    public static Screen getConfigScreen() {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(NiamodClient.mc.currentScreen)
                .setTitle(Text.literal("Nyah Mod"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        ConfigCategory websocket = builder.getOrCreateCategory(Text.of("Websocket"));
        websocket.addEntry(
                entryBuilder
                        .startBooleanToggle(Text.of("Enable"), nyahConfigData.wsEnabled)
                        .setTooltip(Text.of("Enable or disable the websocket connection"))
                        .setDefaultValue(false)
                        .setSaveConsumer(newValue -> nyahConfigData.wsEnabled = newValue)
                        .build());
        websocket.addEntry(
                entryBuilder
                        .startStrField(Text.of("URL"), nyahConfigData.wsURL)
                        .setTooltip(Text.of("Server URL to connect to"))
                        .setDefaultValue("wss://localhost:6767")
                        .setSaveConsumer(newValue -> nyahConfigData.wsURL = newValue)
                        .build());

        ConfigCategory encryption = builder.getOrCreateCategory(Text.of("Encryption"));
        encryption.addEntry(
                entryBuilder
                        .startBooleanToggle(Text.of("Enable"), nyahConfigData.encryptionEnabled)
                        .setTooltip(Text.of("Enable or disable chat encryption"))
                        .setDefaultValue(true)
                        .setSaveConsumer(newValue -> nyahConfigData.encryptionEnabled = newValue)
                        .build());
        encryption.addEntry(
                entryBuilder
                        .startStrField(Text.of("Encryption Prefix"), nyahConfigData.encryptionPrefix)
                        .setTooltip(Text.of("What messages to be encrypted should be prefaced with"))
                        .setDefaultValue("@")
                        .setSaveConsumer(newValue -> nyahConfigData.encryptionPrefix = newValue)
                        .build());
        encryption.addEntry(
                entryBuilder
                        .startStrField(Text.of("Encryption Key"), nyahConfigData.encryptionKey)
                        .setErrorSupplier((key) -> key.isEmpty() ? Optional.of(Text.literal("The encryption key cannot be empty!").withColor(0xFF0000)) : Optional.empty())
                        .setTooltip(Text.of("Encryption Key, should be the same as people you want to secretly message"))
                        .setDefaultValue("six seven")
                        .setSaveConsumer(newValue -> nyahConfigData.encryptionKey = newValue)
                        .build());
        encryption.addEntry(
                entryBuilder
                        .startIntField(Text.of("Salt Length"), nyahConfigData.saltLength)
                        .setTooltip(Text.of("Salt length in bytes"))
                        .setDefaultValue(16)
                        .setSaveConsumer(newValue -> nyahConfigData.saltLength = newValue)
                        .setMin(0)
                        .build());

        ConfigCategory war = builder.getOrCreateCategory(Text.of("War"));
        war.addEntry(
                entryBuilder
                        .startColorField(Text.of("Territory Box Colour"), nyahConfigData.color)
                        .setTooltip(Text.of("Colour of boxes showing queued territory boundaries"))
                        .setDefaultValue(0xFFFFFF)
                        .setSaveConsumer(newValue -> nyahConfigData.color = newValue)
                        .build()
        );
        war.addEntry(
                entryBuilder
                        .startIntField(Text.of("Maximum Territories"), nyahConfigData.maximumTerritories)
                        .setTooltip(Text.of("Maximum territory regions to render at once"))
                        .setDefaultValue(10)
                        .setSaveConsumer(newValue -> nyahConfigData.maximumTerritories = newValue)
                        .build()
        );
        war.addEntry(
                entryBuilder
                        .startIntField(Text.of("Maximum Distance"), nyahConfigData.maximumDistance)
                        .setTooltip(Text.of("Furthest territory box to render at once"))
                        .setDefaultValue(1000)
                        .setSaveConsumer(newValue -> nyahConfigData.maximumDistance = newValue)
                        .build()
        );

        builder.setSavingRunnable(nyahConfigData::save);

        return builder.build();
    }

    private static void getConfigData() {
        try {
            if (!Files.exists(config_file)) {
                Files.createDirectories(config_dir);
                Files.createFile(config_file);
                nyahConfigData = new NyahConfigData();
                nyahConfigData.save();
            }
        } catch (IOException e) {
            e.printStackTrace();
            nyahConfigData = new NyahConfigData();
        }
        try {
            Gson gson = new Gson();
            FileReader reader = new FileReader(config_file.toFile());
            nyahConfigData = gson.fromJson(reader, NyahConfigData.class);
        } catch (IOException e) {
            e.printStackTrace();
            nyahConfigData = new NyahConfigData();
        }
    }


    public static class NyahConfigData {
        public boolean wsEnabled = false;
        public String wsURL = "wss://localhost:6767";

        public boolean encryptionEnabled = true;
        public String encryptionPrefix = "$";
        public String encryptionKey = "six seven";
        public int saltLength = 16;

        public int color = 0xFFFFFF;
        public int maximumDistance = 1000;
        public int maximumTerritories = 10;

        public NyahConfigData() {
        }

        public NyahConfigData(boolean wsEnabled, String wsURL, boolean encryptionEnabled, String encryptionPrefix, String encryptionKey, int saltLength, int color, int maximumDistance, int maximumTerritories) {
            this.wsEnabled = wsEnabled;
            this.wsURL = wsURL;
            this.encryptionEnabled = encryptionEnabled;
            this.encryptionPrefix = encryptionPrefix;
            this.encryptionKey = encryptionKey;
            this.saltLength = saltLength;
            this.color = color;
            this.maximumDistance = maximumDistance;
            this.maximumTerritories = maximumTerritories;
        }

        public void save() {
            try {
                Gson gson = new Gson();
                FileWriter writer = new FileWriter(config_file.toFile());
                gson.toJson(this, writer);
                writer.flush();
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}