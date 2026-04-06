package org.nia.niamod.features;

import net.minecraft.network.chat.Component;
import org.nia.niamod.eventbus.NiaEventBus;
import org.nia.niamod.eventbus.Subscribe;
import org.nia.niamod.models.events.BossBarNameEvent;
import org.nia.niamod.models.misc.Feature;
import org.nia.niamod.models.misc.Safe;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WarTowerEHPFeature extends Feature {

    private static final Pattern towerRegex = Pattern.compile(
            "§3\\[([A-Za-z]{3,4})\\] §b([A-Za-z ]*)§7 - §4❤ ([0-9]+)§7 \\(§6([0-9.]+)%§7\\) - §c☠ ([0-9]+)-([0-9]+)§7 \\(§b([0-9]\\.[0-9]*)x§7\\)"
    );

    private Component replaceEHP(Component text) {
        Matcher matcher = towerRegex.matcher(text.getString());
        if (matcher.matches()) {
            String tag = matcher.group(1);
            String name = matcher.group(2);
            int hp = Integer.parseInt(matcher.group(3));
            double percent = Double.parseDouble(matcher.group(4));
            int atckLow = Integer.parseInt(matcher.group(5));
            int atckHigh = Integer.parseInt(matcher.group(6));
            double speed = Double.parseDouble(matcher.group(7));

            double ehp = hp / (1 - (percent / 100.0));

            return Component.nullToEmpty(
                    "§3[" + tag + "] §b" + name + "§7 - §4❤ " + (int) ehp +
                            "§7 - §c☠ " + atckLow + "-" + atckHigh +
                            "§7 (§b" + speed + "x§7)");
        } else {
            return text;
        }
    }

    @Subscribe
    @Safe
    public void onBossBarName(BossBarNameEvent event) {
        event.setTitle(replaceEHP(event.getTitle()));
    }

    @Override
    @Safe
    public void init() {
        NiaEventBus.subscribe(this);
    }
}
