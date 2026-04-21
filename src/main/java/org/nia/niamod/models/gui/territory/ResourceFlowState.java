package org.nia.niamod.models.gui.territory;

public record ResourceFlowState(Resources gain, Resources capacity, Resources current, Resources gainedInHour, Resources lostInHour) {
    public static final ResourceFlowState EMPTY = new ResourceFlowState(Resources.EMPTY, Resources.EMPTY, Resources.EMPTY, Resources.EMPTY, Resources.EMPTY);

    public boolean hasAnyCapacity() {
        return capacity.emeralds() > 0
                || capacity.ore() > 0
                || capacity.crops() > 0
                || capacity.fish() > 0
                || capacity.wood() > 0;
    }

    public boolean hasAnyData() {
        return hasAnyCapacity();
    }
}
