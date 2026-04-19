package org.nia.niamod.features;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.eventbus.NiaEventBus;
import org.nia.niamod.eventbus.Subscribe;
import org.nia.niamod.managers.KeybindManager;
import org.nia.niamod.managers.Scheduler;
import org.nia.niamod.models.events.ChatMessageReceivedEvent;
import org.nia.niamod.models.gui.IgnoreManagerScreen;
import org.nia.niamod.models.ignore.IgnoreAction;
import org.nia.niamod.models.ignore.IgnorePlayerEntry;
import org.nia.niamod.models.ignore.IgnorePlayerMode;
import org.nia.niamod.models.misc.Feature;
import org.nia.niamod.models.misc.Safe;
import org.nia.niamod.util.WynncraftAPI;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class IgnoreFeature extends Feature {
    private static final int IGNORE_GAP_TICKS = 10;
    private static final Pattern CHAT_PLAYER_PATTERN = Pattern.compile("\uDAFF\uDFFC\uE008\uDAFF\uDFFF\uE002\uDAFF\uDFFE ([A-Za-z0-9]{3,16}) has been added to your ignore list!");
    private static final Pattern CHAT_ALREADY_IGNORED_PATTERN = Pattern.compile("\uDAFF\uDFFC\uE008\uDAFF\uDFFF\uE002\uDAFF\uDFFE ([A-Za-z0-9]{3,16}) is already ignored!");
    private static final Pattern CHAT_UNIGNORED_PATTERN = Pattern.compile("\uDAFF\uDFFC\uE008\uDAFF\uDFFF\uE002\uDAFF\uDFFE ([A-Za-z0-9]{3,16}) has been removed from your ignore list!");

    private final Set<String> ignoredPlayers = new HashSet<>();
    private final Set<String> chatDetectedPlayers = new HashSet<>();
    private int ignoreRun;
    @Getter
    private IgnoreAction nextFavouriteAction = IgnoreAction.IGNORE;
    @Getter
    private int revision;
    @Getter
    private List<String> players;

    @Override
    public void init() {
        KeybindManager.registerKeybinding(
                "Open Ignore Manager",
                GLFW.GLFW_KEY_BACKSLASH,
                safeRunnable("open_manager", this::openScreen)
        );
        KeybindManager.registerKeybinding(
                "Mass Ignore",
                GLFW.GLFW_KEY_RIGHT_BRACKET,
                safeRunnable("toggle_favourite_ignores", this::toggleFavourites)
        );
        NiaEventBus.subscribe(this);
        players = WynncraftAPI.guildResponse(NyahConfig.getData().getGuildName()).allUsernames();
    }

    public List<IgnorePlayerEntry> getVisiblePlayers(String searchQuery) {
        String query = normalize(searchQuery);
        return listedPlayers().stream()
                .filter(entry -> query.isEmpty() || normalize(entry.playerName()).contains(query))
                .sorted(Comparator
                        .comparingInt(this::rowRank)
                        .thenComparing(entry -> normalize(entry.playerName())))
                .toList();
    }

    public IgnorePlayerMode modeFor(String playerName) {
        if (isFavourite(playerName)) {
            return IgnorePlayerMode.FAVOURITE;
        }
        if (isAvoided(playerName)) {
            return IgnorePlayerMode.AVOID;
        }
        return IgnorePlayerMode.NONE;
    }

    public boolean isFavourite(String playerName) {
        return containsName(NyahConfig.getData().getFavouritePlayers(), playerName);
    }

    public boolean isAvoided(String playerName) {
        return containsName(NyahConfig.getData().getAvoidedPlayers(), playerName);
    }

    public boolean canChangeMode(String playerName) {
        return !isChatDetected(playerName);
    }

    public void setMode(String playerName, IgnorePlayerMode mode) {
        if (!canChangeMode(playerName)) {
            return;
        }
        removeName(NyahConfig.getData().getFavouritePlayers(), playerName);
        removeName(NyahConfig.getData().getAvoidedPlayers(), playerName);
        if (mode == IgnorePlayerMode.FAVOURITE) {
            addName(NyahConfig.getData().getFavouritePlayers(), playerName);
        } else if (mode == IgnorePlayerMode.AVOID) {
            addName(NyahConfig.getData().getAvoidedPlayers(), playerName);
        }
        NyahConfig.save();
        markChanged();
    }

    public void setFavourite(String playerName) {
        setMode(playerName, IgnorePlayerMode.FAVOURITE);
    }

    public void setAvoided(String playerName) {
        setMode(playerName, IgnorePlayerMode.AVOID);
    }

    public void clearMode(String playerName) {
        setMode(playerName, IgnorePlayerMode.NONE);
    }

    public void addChatDetectedPlayer(String playerName) {
        if (playerName != null && !playerName.isBlank()) {
            String trimmed = playerName.trim();
            boolean detectedChanged = addChatDetected(trimmed);
            boolean ignoredChanged = setIgnored(trimmed, true);
            if (detectedChanged || ignoredChanged) {
                markChanged();
            }
        }
    }

    public void clearChatDetectedPlayers() {
        if (!chatDetectedPlayers.isEmpty()) {
            chatDetectedPlayers.clear();
            markChanged();
        }
    }

    public boolean isIgnored(String playerName) {
        return ignoredPlayers.contains(normalize(playerName));
    }

    public boolean hasIgnoredPlayers() {
        return !ignoredPlayers.isEmpty();
    }

    public void markIgnored(String playerName) {
        setIgnoredState(playerName, true);
    }

    public void markUnignored(String playerName) {
        setIgnoredState(playerName, false);
    }

    public void setIgnoredState(String playerName, boolean ignored) {
        if (setIgnored(playerName, ignored)) {
            markChanged();
        }
    }

    public void toggleIgnored(String playerName) {
        String normalized = normalize(playerName);
        boolean ignored = ignoredPlayers.contains(normalized);
        sendIgnoreCommand(playerName, ignored);
        if (ignored) {
            ignoredPlayers.remove(normalized);
        } else {
            ignoredPlayers.add(normalized);
        }
        markChanged();
    }

    public void toggleFavourites() {
        IgnoreAction action = nextFavouriteAction;
        setNextFavouriteAction(action.opposite());
        runFavouriteAction(action);
    }

    public void setNextFavouriteAction(IgnoreAction action) {
        if (action != null && nextFavouriteAction != action) {
            nextFavouriteAction = action;
            markChanged();
        }
    }

    public void runFavouriteAction(IgnoreAction action) {
        queue(cleanNames(NyahConfig.getData().getFavouritePlayers()), action);
    }

    public void unignoreEveryone() {
        queue(ignoredNames(), IgnoreAction.UNIGNORE);
    }

    @Subscribe
    @Safe
    public void onChatMessage(ChatMessageReceivedEvent event) {
        if (event.message() != null) {
            addPlayerFromChatMessage(event.message().getString());
        }
    }

    public void addPlayerFromChatMessage(String message) {
        if (message == null || message.isBlank()) {
            return;
        }

        applyPlayerPattern(message, CHAT_PLAYER_PATTERN, this::markIgnoredFromChat);
        applyPlayerPattern(message, CHAT_ALREADY_IGNORED_PATTERN, this::markIgnored);
        applyPlayerPattern(message, CHAT_UNIGNORED_PATTERN, this::markUnignored);
    }

    public void markIgnoredFromChat(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return;
        }

        String trimmed = playerName.trim();
        boolean ignoredChanged = setIgnored(trimmed, true);
        boolean detectedChanged = false;
        if (!isKnownPlayer(trimmed)) {
            detectedChanged = addChatDetected(trimmed);
        }
        if (ignoredChanged || detectedChanged) {
            markChanged();
        }
    }

    private void applyPlayerPattern(String message, Pattern pattern, Consumer<String> action) {
        Matcher matcher = pattern.matcher(message);
        if (!matcher.find()) {
            return;
        }

        String playerName = playerNameFromMatcher(matcher);
        if (playerName != null && !playerName.isBlank()) {
            action.accept(playerName);
        }
    }

    private String playerNameFromMatcher(Matcher matcher) {
        try {
            String namedGroup = matcher.group("player");
            if (namedGroup != null && !namedGroup.isBlank()) {
                return namedGroup;
            }
        } catch (IllegalArgumentException ignored) {
        }
        if (matcher.groupCount() >= 1) {
            return matcher.group(1);
        }
        return matcher.group();
    }

    private List<IgnorePlayerEntry> listedPlayers() {
        Map<String, IgnorePlayerEntry> entries = new LinkedHashMap<>();
        addKnownPlayers(entries, getPlayers());
        addKnownPlayers(entries, NyahConfig.getData().getFavouritePlayers());
        addKnownPlayers(entries, NyahConfig.getData().getAvoidedPlayers());
        addChatPlayers(entries, chatDetectedPlayers);
        return new ArrayList<>(entries.values());
    }

    private void addKnownPlayers(Map<String, IgnorePlayerEntry> entries, List<String> names) {
        if (names == null) {
            return;
        }
        for (String name : names) {
            if (name == null || name.isBlank()) {
                continue;
            }
            String trimmed = name.trim();
            entries.putIfAbsent(normalize(trimmed), player(trimmed, canChangeMode(trimmed)));
        }
    }

    private void addChatPlayers(Map<String, IgnorePlayerEntry> entries, Set<String> names) {
        for (String name : names) {
            if (name == null || name.isBlank()) {
                continue;
            }
            String trimmed = name.trim();
            entries.put(normalize(trimmed), player(trimmed, false));
        }
    }

    private IgnorePlayerEntry player(String playerName, boolean modeEditable) {
        return new IgnorePlayerEntry(playerName, modeFor(playerName), isIgnored(playerName), modeEditable);
    }

    private boolean addChatDetected(String playerName) {
        String trimmed = playerName.trim();
        boolean removed = chatDetectedPlayers.removeIf(name -> normalize(name).equals(normalize(trimmed)));
        boolean added = chatDetectedPlayers.add(trimmed);
        return removed || added;
    }

    private int rowRank(IgnorePlayerEntry entry) {
        if (entry.mode() == IgnorePlayerMode.FAVOURITE) {
            return 0;
        }
        if (entry.ignored() && entry.modeEditable()) {
            return 1;
        }
        if (!entry.modeEditable()) {
            return 2;
        }
        return entry.mode().getSortOrder();
    }

    private boolean isChatDetected(String playerName) {
        String normalized = normalize(playerName);
        return chatDetectedPlayers.stream().anyMatch(name -> normalize(name).equals(normalized));
    }

    private boolean isKnownPlayer(String playerName) {
        return containsName(getPlayers(), playerName)
                || containsName(NyahConfig.getData().getFavouritePlayers(), playerName)
                || containsName(NyahConfig.getData().getAvoidedPlayers(), playerName)
                || isChatDetected(playerName);
    }

    private boolean setIgnored(String playerName, boolean ignored) {
        if (playerName == null || playerName.isBlank()) {
            return false;
        }

        String normalized = normalize(playerName);
        if (ignored) {
            return ignoredPlayers.add(normalized);
        }
        return ignoredPlayers.remove(normalized);
    }

    private void markChanged() {
        revision++;
    }

    private void addName(List<String> names, String playerName) {
        if (names != null && !containsName(names, playerName)) {
            names.add(playerName);
        }
    }

    private void removeName(List<String> names, String playerName) {
        if (names != null) {
            names.removeIf(name -> normalize(name).equals(normalize(playerName)));
        }
    }

    private List<String> cleanNames(List<String> names) {
        Map<String, String> uniqueNames = new LinkedHashMap<>();
        if (names != null) {
            for (String name : names) {
                if (name == null || name.isBlank()) {
                    continue;
                }
                String trimmed = name.trim();
                uniqueNames.putIfAbsent(normalize(trimmed), trimmed);
            }
        }
        return new ArrayList<>(uniqueNames.values());
    }

    private List<String> ignoredNames() {
        Map<String, String> names = new LinkedHashMap<>();
        listedPlayers().stream()
                .filter(IgnorePlayerEntry::ignored)
                .forEach(entry -> names.putIfAbsent(normalize(entry.playerName()), entry.playerName()));
        ignoredPlayers.forEach(name -> names.putIfAbsent(name, name));
        return new ArrayList<>(names.values());
    }

    private boolean containsName(List<String> names, String playerName) {
        if (names == null) {
            return false;
        }
        String normalized = normalize(playerName);
        return names.stream().anyMatch(name -> normalize(name).equals(normalized));
    }

    private void openScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new IgnoreManagerScreen(minecraft.screen, this));
    }

    private void queue(List<String> playerNames, IgnoreAction action) {
        if (action == null) {
            return;
        }

        int runId = ++ignoreRun;
        for (int index = 0; index < playerNames.size(); index++) {
            String playerName = playerNames.get(index);
            int delayTicks = index * IGNORE_GAP_TICKS;
            Scheduler.schedule(
                    safeRunnable("ignore_action", () -> runAction(playerName, action, runId)),
                    delayTicks
            );
        }
    }

    private void runAction(String playerName, IgnoreAction action, int runId) {
        if (runId != ignoreRun) {
            return;
        }

        sendIgnoreCommand(playerName, action);
        if (setIgnored(playerName, action.isIgnoredState())) {
            markChanged();
        }
    }

    private void sendIgnoreCommand(String playerName, boolean currentlyIgnored) {
        sendIgnoreCommand(playerName, currentlyIgnored ? IgnoreAction.UNIGNORE : IgnoreAction.IGNORE);
    }

    private void sendIgnoreCommand(String playerName, IgnoreAction action) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) {
            return;
        }
        minecraft.getConnection().sendCommand("ignore " + action.getCommandAction() + " " + playerName);
    }

    private String normalize(String playerName) {
        return playerName == null ? "" : playerName.trim().toLowerCase(Locale.ROOT);
    }
}
