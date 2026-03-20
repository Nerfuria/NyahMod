package org.nia.niamod.features;

import net.minecraft.text.Text;
import org.nia.niamod.config.NyahConfig;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WarTowerEHPFeature {

    private static final Pattern towerRegex = Pattern.compile(
            "§3\\[([A-Za-z]{3,4})\\] §b([A-Za-z ]*)§7 - §4❤ ([0-9]+)§7 \\(§6([0-9.]+)%§7\\) - §c☠ ([0-9]+)-([0-9]+)§7 \\(§b([0-9]\\.[0-9]*)x§7\\)"
    );

    public Text replaceEHP(Text text) {
        if (!NyahConfig.nyahConfigData.replaceTowerHP) return text;
        Matcher matcher = towerRegex.matcher(text.getString());
        StringBuffer sb = new StringBuffer();
        if (matcher.matches()) {
            String tag = matcher.group(1);
            String name = matcher.group(2);
            int hp = Integer.parseInt(matcher.group(3));
            double percent = Double.parseDouble(matcher.group(4));
            int atckLow = Integer.parseInt(matcher.group(5));
            int atckHigh = Integer.parseInt(matcher.group(6));
            double speed = Double.parseDouble(matcher.group(7));

            double newValue = hp / (1 - (percent / 100.0));

            String replacement =
                    "§3[" + tag + "] §b" + name + "§7 - §4❤ " + newValue +
                            "§7 - §c☠ " + atckLow + "-" + atckHigh +
                            "§7 (§b" + speed + "x§7)";

            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);

        return Text.of(sb.toString());
    }
}
