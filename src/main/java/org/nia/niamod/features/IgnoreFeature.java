package org.nia.niamod.features;

import lombok.Getter;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.nia.niamod.NiamodClient;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.eventbus.NiaEventBus;
import org.nia.niamod.eventbus.Subscribe;
import org.nia.niamod.managers.KeybindManager;
import org.nia.niamod.managers.Scheduler;
import org.nia.niamod.models.events.ChatMessageReceivedEvent;
import org.nia.niamod.models.events.PostInitEvent;
import org.nia.niamod.models.misc.Feature;
import org.nia.niamod.models.misc.Safe;
import org.nia.niamod.models.records.State;
import org.nia.niamod.util.WynncraftAPI;

import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.nia.niamod.config.NyahConfig.nyahConfigData;

public class IgnoreFeature extends Feature {
    @Getter
    private List<String> guildMembers = List.of();

    private HashMap<String, Boolean> ignored;
    private boolean globalIgnore;
    private String loadedGuildName = "";
    private Pattern ignoreAddRegex;
    private Pattern ignoreRemoveRegex;

    @Safe
    public void init() {
        ignored = new HashMap<>();
        globalIgnore = false;
        KeybindManager.registerKeybinding("Ignore All", GLFW.GLFW_KEY_DELETE, safeRunnable("ignoreAll", this::ignoreAll));
        ignoreAddRegex = Pattern.compile("\uDAFF\uDFFC\uE008\uDAFF\uDFFF\uE002\uDAFF\uDFFE ([A-Za-z0-9]{3,16}) has been added to your ignore list!");
        ignoreRemoveRegex = Pattern.compile("\uDAFF\uDFFC\uE008\uDAFF\uDFFF\uE002\uDAFF\uDFFE ([A-Za-z0-9]{3,16}) has been removed from your ignore list!");
        NiaEventBus.subscribe(this);
    }

    @Subscribe
    @Safe
    public void postInit(PostInitEvent ignoredEvent) {
        if (isEnabled()) {
            syncGuildMembers();
        }
    }

    @Subscribe
    @Safe
    public void processMessage(ChatMessageReceivedEvent event) {
        Component message = event.message();
        String text = message.getString();

        Matcher ignoreAdd = ignoreAddRegex.matcher(text);
        if (ignoreAdd.find()) {
            ignored.put(ignoreAdd.group(1), true);
        }

        Matcher ignoreRemove = ignoreRemoveRegex.matcher(text);
        if (ignoreRemove.find()) {
            ignored.put(ignoreRemove.group(1), false);
        }
    }

    @Safe
    public void syncGuildMembers() {
        if (isDisabled()) {
            return;
        }

        runSafe("syncGuildMembers", () -> {
            String guildName = nyahConfigData.getGuildName().trim();

            if (guildName.isEmpty()) {
                loadedGuildName = guildName;
                guildMembers = List.of();
                return;
            }

            if (guildName.equalsIgnoreCase(loadedGuildName) && !guildMembers.isEmpty()) {
                return;
            }

            guildMembers = WynncraftAPI.guildResponse(guildName).allUsernames().stream()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
            loadedGuildName = guildName;
            nyahConfigData.getFavouritePlayers().removeIf(name -> !guildMembers.contains(name));
            nyahConfigData.getAvoidedPlayers().removeIf(name -> !guildMembers.contains(name));
            NyahConfig.save();
        });
    }

    @Safe
    public void ignoreAll() {
        if (isDisabled()) {
            return;
        }

        globalIgnore = !globalIgnore;
        for (int i = 0; i < nyahConfigData.getFavouritePlayers().size(); i++) {
            String username = nyahConfigData.getFavouritePlayers().get(i);
            Scheduler.schedule(() -> ignore(username, globalIgnore), i * 10);
        }
    }

    @Safe
    public void ignore(String username, boolean ignore) {
        if (NiamodClient.mc.getConnection() != null) {
            NiamodClient.mc.getConnection().sendCommand("ignore " + (ignore ? "add " : "remove ") + username);
        }
        ignored.put(username, ignore);
    }

    public boolean isIgnored(String username) {
        return ignored.getOrDefault(username, false);
    }

    public State getState(String username) {
        if (nyahConfigData.getFavouritePlayers().contains(username)) return State.FAVOURITE;
        if (nyahConfigData.getAvoidedPlayers().contains(username)) return State.AVOID;
        return State.NORMAL;
    }

    @Safe
    public void cycleState(String username) {
        State nextState = switch (getState(username)) {
            case NORMAL -> State.FAVOURITE;
            case FAVOURITE -> State.AVOID;
            case AVOID -> State.NORMAL;
        };
        setState(username, nextState);
    }

    @Safe
    public void setState(String username, State state) {
        nyahConfigData.getFavouritePlayers().remove(username);
        nyahConfigData.getAvoidedPlayers().remove(username);

        switch (state) {
            case FAVOURITE -> nyahConfigData.getFavouritePlayers().add(username);
            case AVOID -> nyahConfigData.getAvoidedPlayers().add(username);
            case NORMAL -> {
            }
        }

        NyahConfig.save();
    }
}
