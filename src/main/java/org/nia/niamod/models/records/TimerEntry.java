package org.nia.niamod.models.records;

import com.wynntils.models.territories.TerritoryAttackTimer;

public record TimerEntry(TerritoryAttackTimer timer, Territory territory, int distance) {
}