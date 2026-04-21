package org.nia.niamod.models.gui.territory;

import org.nia.niamod.models.gui.theme.ClickGuiTheme;

public enum TreasuryLevel {
    LOW("Low");

    private final String label;

    TreasuryLevel(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public int color(ClickGuiTheme theme) {
        return theme.trinaryText();
    }
}
