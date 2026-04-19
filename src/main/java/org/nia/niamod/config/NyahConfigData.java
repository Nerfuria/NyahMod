package org.nia.niamod.config;

import lombok.Getter;
import lombok.Setter;
import org.nia.niamod.models.config.ClickGuiAnimationMode;
import org.nia.niamod.models.config.RadianceOverlayMode;
import org.nia.niamod.models.config.ShoutReplacement;
import org.nia.niamod.models.gui.theme.ClickGuiFontOption;
import org.nia.niamod.models.gui.theme.ClickGuiThemeOption;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class NyahConfigData {
    private String apiBase = "https://api.wynncraft.com/v3/";
    private String guildName = "Nerfuria";

    private String clickGuiTheme = ClickGuiThemeOption.DEFAULT.getKey();
    private String clickGuiFont = ClickGuiFontOption.MINECRAFT_DEFAULT.getKey();
    private ClickGuiAnimationMode clickGuiAnimation = ClickGuiAnimationMode.NONE;
    private int animationTime = 1000;
    private float guiOpacity = 0.9f;
    private int customGuiBackground = 0x171A21;
    private int customGuiSecondary = 0x11141B;
    private int customGuiAccent = 0x4794FD;
    private int guiWidth = 500;
    private int guiHeight = 350;

    private boolean resourceTickFeatureEnabled = true;
    private boolean chatEncryptionFeatureEnabled = true;
    private boolean autoStreamFeatureEnabled = true;
    private boolean warTimersFeatureEnabled = true;
    private boolean warTowerEhpFeatureEnabled = true;
    private boolean consuTextFeatureEnabled = true;
    private boolean shoutFilterFeatureEnabled = true;
    private boolean viewModelFeatureEnabled = true;
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

    public void normalize() {
        if (favouritePlayers == null) {
            favouritePlayers = new ArrayList<>();
        }
        if (avoidedPlayers == null) {
            avoidedPlayers = new ArrayList<>();
        }

        clickGuiTheme = ClickGuiThemeOption.resolve(clickGuiTheme).getKey();
        clickGuiFont = ClickGuiFontOption.resolve(clickGuiFont).getKey();

        if (clickGuiAnimation == null) {
            clickGuiAnimation = ClickGuiAnimationMode.NONE;
        }
        if (shoutFilterMode == null) {
            shoutFilterMode = ShoutReplacement.COLLAPSE;
        }
        if (radianceSyncOverlayMode == null) {
            radianceSyncOverlayMode = RadianceOverlayMode.CAST;
        }
        if (radianceSyncGroupKey == null) {
            radianceSyncGroupKey = "";
        }
        if (radianceSyncWorkerUrl == null || radianceSyncWorkerUrl.isBlank()) {
            radianceSyncWorkerUrl = "https://radiancesync.wavelink.workers.dev";
        }
    }
}
