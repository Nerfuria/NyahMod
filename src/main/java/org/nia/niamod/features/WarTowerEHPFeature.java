package org.nia.niamod.features;

import net.minecraft.text.Text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WarTowerEHPFeature {

    private static final Pattern towerRegex = Pattern.compile(
            "§4❤ ([0-9]+)§7 \\(§6([0-9.]+)%§7\\)"
    );

    public Text replaceEHP(Text text) {
        Matcher matcher = towerRegex.matcher(text.getString());

        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            int number = Integer.parseInt(matcher.group(1));
            double percent = Double.parseDouble(matcher.group(2)) / 100;
            System.out.println(number);
            System.out.println(percent);
            double newValue = number / (1 - percent);

            String replacement = "§4❤  " + newValue + "§7";

            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);

        return Text.of(sb.toString());
    }
}
