package org.nia.niamod.features;

import me.shedaniel.clothconfig2.api.AbstractConfigEntry;
import me.shedaniel.clothconfig2.gui.ClothConfigScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.nia.niamod.NiamodClient;
import org.nia.niamod.managers.KeybindManager;
import org.nia.niamod.managers.Scheduler;
import org.nia.niamod.models.events.ChatEvent;
import org.nia.niamod.models.gui.IgnoreEntry;
import org.nia.niamod.models.gui.SeparatorEntry;
import org.nia.niamod.models.misc.Feature;
import org.nia.niamod.models.misc.Safe;
import org.nia.niamod.models.records.State;
import org.nia.niamod.util.WynncraftAPI;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.nia.niamod.config.NyahConfig.nyahConfigData;

public class IgnoreFeature extends Feature {

    private ClothConfigScreen screen;
    private boolean updated = true;
    private List<AbstractConfigEntry<AbstractConfigEntry<?>>> cache;
    private List<IgnoreEntry> entries;
    private HashMap<String, Boolean> ignored;
    private boolean globalIgnore;

    private Pattern ignoreAddRegex;
    private Pattern ignoreRemoveRegex;

    @Safe
    public void init() {
        ignored = new HashMap<>();
        globalIgnore = false;
        KeybindManager.registerKeybinding("Ignore All", GLFW.GLFW_KEY_DELETE, this::ignoreAll);
        ignoreAddRegex = Pattern.compile("\uDAFF\uDFFC\uE008\uDAFF\uDFFF\uE002\uDAFF\uDFFE ([A-Za-z0-9]{3,16}) has been added to your ignore list!");
        ignoreRemoveRegex = Pattern.compile("\uDAFF\uDFFC\uE008\uDAFF\uDFFF\uE002\uDAFF\uDFFE ([A-Za-z0-9]{3,16}) has been removed from your ignore list!");
        ChatEvent.RECIEVED.register(this::processMessage);
    }

    public void postInit() {
        entries = WynncraftAPI.guildResponse(nyahConfigData.guildName).allUsernames().stream().map(this::ignoreEntry).toList();
    }

    @Safe
    public void processMessage(Text message) {
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

    public void setScreen(ClothConfigScreen screen) {
        this.screen = screen;
        sortEntries();
    }

    public List<IgnoreEntry> getIgnoreEntries() {
        return entries;
    }

    private IgnoreEntry ignoreEntry(String username) {
        return new IgnoreEntry(
                Text.of(username),
                null,
                btn -> ignore(username, !ignored.getOrDefault(username, false)),
                btn -> setState(username, State.FAVOURITE, btn),
                btn -> setState(username, State.AVOID, btn),
                btn -> setState(username, State.NORMAL, btn),
                stateOf(username)
        );
    }

    public void ignoreAll() {
        globalIgnore = !globalIgnore;
        for (int i = 0; i < nyahConfigData.favouritePlayers.size(); i++) {
            String username = nyahConfigData.favouritePlayers.get(i);
            Scheduler.schedule(() -> ignore(username, globalIgnore), i);
        }
    }

    public void ignore(String username, boolean ignore) {
        NiamodClient.mc.getNetworkHandler().sendChatCommand("ignore " + (ignore ? "add " : "remove ") + username);
        ignored.put(username, ignore);
    }

    private State stateOf(String name) {
        if (nyahConfigData.favouritePlayers.contains(name)) return State.FAVOURITE;
        if (nyahConfigData.avoidedPlayers.contains(name)) return State.AVOID;
        return State.NORMAL;
    }

    private void setState(String username, State newState, ButtonWidget button) {
        nyahConfigData.favouritePlayers.remove(username);
        nyahConfigData.avoidedPlayers.remove(username);

        switch (newState) {
            case FAVOURITE -> nyahConfigData.favouritePlayers.add(username);
            case AVOID -> nyahConfigData.avoidedPlayers.add(username);
            case NORMAL -> {
            }
        }

        button.setMessage(Text.of(newState.code + "♥"));
        updated = true;

        sortEntries();
    }

    private void sortEntries() {
        screen.listWidget.entriesTransformer = list -> {
            if (!screen.getSelectedCategory().getString().equals("Ignore")) {
                return list.stream()
                        .filter(entry -> entry.getClass() == SeparatorEntry.class
                                ? list.subList(list.indexOf(entry) + 1, list.size()).stream()
                                .takeWhile(next -> next.getClass() != SeparatorEntry.class)
                                .anyMatch(next -> screen.matchesSearch(next.getSearchTags()))
                                : screen.matchesSearch(entry.getSearchTags()))
                        .toList();
            }

            if (!updated) {
                return cache.stream().filter(entry -> screen.matchesSearch(entry.getSearchTags())).toList();
            }

            updated = false;

            List<AbstractConfigEntry<AbstractConfigEntry<?>>> players = list.subList(3, list.size());

            Set<String> favSet = new HashSet<>(nyahConfigData.favouritePlayers);
            Set<String> avoidSet = new HashSet<>(nyahConfigData.avoidedPlayers);

            Comparator<AbstractConfigEntry<AbstractConfigEntry<?>>> comparator = Comparator
                    .<AbstractConfigEntry<AbstractConfigEntry<?>>, Integer>comparing(e -> {
                        String n = e.getFieldName().getString();
                        if (favSet.contains(n)) return 0;
                        if (avoidSet.contains(n)) return 2;
                        return 1;
                    })
                    .thenComparing(e -> e.getFieldName().getString(), String.CASE_INSENSITIVE_ORDER);

            List<AbstractConfigEntry<AbstractConfigEntry<?>>> result = new ArrayList<>(list.subList(0, 3));
            result.addAll(players.stream().filter(entry -> screen.matchesSearch(entry.getSearchTags())).sorted(comparator).toList());
            cache = result;
            return result;
        };
    }

}