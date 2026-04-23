package org.nia.niamod.models.eco;

public record ResourceFlow(
        ResourceAmounts stored,
        ResourceAmounts capacity,
        ResourceAmounts gainedPerHour,
        ResourceAmounts usedPerHour,
        double materialUsagePercent
) {
    public static final ResourceFlow EMPTY = new ResourceFlow(
            ResourceAmounts.EMPTY,
            ResourceAmounts.EMPTY,
            ResourceAmounts.EMPTY,
            ResourceAmounts.EMPTY,
            0.0
    );

    public ResourceFlow {
        stored = stored == null ? ResourceAmounts.EMPTY : stored;
        capacity = capacity == null ? ResourceAmounts.EMPTY : capacity;
        gainedPerHour = gainedPerHour == null ? ResourceAmounts.EMPTY : gainedPerHour;
        usedPerHour = usedPerHour == null ? ResourceAmounts.EMPTY : usedPerHour;
    }

    public ResourceAmounts netPerHour() {
        return gainedPerHour.minus(usedPerHour);
    }
}
