package org.nia.niamod.models.gui.territory;

public record ResourceFlowState(
        String headquarterName,
        long headquarterResources,
        long headquarterCapacity,
        double resourceGainPerHour,
        long resourceLossPerHour,
        double emeraldGainPerHour,
        double resourceUsagePercent
) {
    public static final ResourceFlowState EMPTY = new ResourceFlowState(null, 0L, 0L, 0.0, 0L, 0.0, 0.0);

    public double netResourcePerHour() {
        return resourceGainPerHour - resourceLossPerHour;
    }

    public boolean hasHeadquarters() {
        return headquarterName != null && !headquarterName.isBlank();
    }
}
