package org.nia.niamod.managers;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class KeybindManager {

    private static KeyBinding.Category CATEGORY;

    private static HashMap<KeyBinding, Runnable> keybinds;

    public static void init() {
        CATEGORY = KeyBinding.Category.create(Identifier.of("niamod", "config"));

        keybinds = new HashMap<>();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                for (Map.Entry<KeyBinding, Runnable> entry : keybinds.entrySet()) {
                    if (entry.getKey().isPressed()) {
                        entry.getValue().run();
                    }
                }
            }
        });
    }

    public static void registerKeybinding(String name, int key, Runnable action) {
        KeyBinding keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(name, InputUtil.Type.KEYSYM, key, CATEGORY));
        keybinds.put(keyBinding, action);
    }
}
