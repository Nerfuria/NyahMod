package org.nia.niamod.features;

import me.shedaniel.clothconfig2.api.AbstractConfigEntry;
import me.shedaniel.clothconfig2.gui.ClothConfigScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import org.nia.niamod.models.gui.IgnoreEntry;
import org.nia.niamod.models.gui.SeparatorEntry;
import org.nia.niamod.util.WynncraftAPI;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.nia.niamod.config.NyahConfig.nyahConfigData;

public class IgnoreFeature {

    private ClothConfigScreen screen;
    private boolean updated = true;
    private List<AbstractConfigEntry<AbstractConfigEntry<?>>> cache;
    private List<IgnoreEntry> entries;

    public void init() {
        entries = WynncraftAPI.guildResponse(nyahConfigData.guildName).allUsernames().stream().map(this::ignoreEntry).toList();
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
                btn -> { /* TODO: implement ignore logic */ },
                btn -> setState(username, IgnoreEntry.State.FAVOURITE, btn),
                btn -> setState(username, IgnoreEntry.State.AVOID, btn),
                btn -> setState(username, IgnoreEntry.State.NORMAL, btn),
                stateOf(username)
        );
    }

    private IgnoreEntry.State stateOf(String name) {
        if (nyahConfigData.favouritePlayers.contains(name)) return IgnoreEntry.State.FAVOURITE;
        if (nyahConfigData.avoidedPlayers.contains(name)) return IgnoreEntry.State.AVOID;
        return IgnoreEntry.State.NORMAL;
    }

    private void setState(String username, IgnoreEntry.State newState, ButtonWidget button) {
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
