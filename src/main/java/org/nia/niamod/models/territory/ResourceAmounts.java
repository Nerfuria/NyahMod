package org.nia.niamod.models.territory;

public record ResourceAmounts(long emeralds, long ore, long crops, long fish, long wood) {
    public static final ResourceAmounts EMPTY = new ResourceAmounts(0L, 0L, 0L, 0L, 0L);

    public static ResourceAmounts of(ResourceKind kind, long amount) {
        long safeAmount = Math.max(0L, amount);
        return switch (kind) {
            case EMERALDS -> new ResourceAmounts(safeAmount, 0L, 0L, 0L, 0L);
            case ORE -> new ResourceAmounts(0L, safeAmount, 0L, 0L, 0L);
            case CROPS -> new ResourceAmounts(0L, 0L, safeAmount, 0L, 0L);
            case FISH -> new ResourceAmounts(0L, 0L, 0L, safeAmount, 0L);
            case WOOD -> new ResourceAmounts(0L, 0L, 0L, 0L, safeAmount);
            case ALL -> new ResourceAmounts(safeAmount, safeAmount, safeAmount, safeAmount, safeAmount);
            case NONE -> EMPTY;
        };
    }

    public static ResourceAmounts fromResources(Resources resources) {
        if (resources == null) {
            return EMPTY;
        }
        return new ResourceAmounts(resources.emeralds(), resources.ore(), resources.crops(), resources.fish(), resources.wood());
    }

    public ResourceAmounts plus(ResourceAmounts other) {
        if (other == null) {
            return this;
        }
        return new ResourceAmounts(
                emeralds + other.emeralds,
                ore + other.ore,
                crops + other.crops,
                fish + other.fish,
                wood + other.wood
        );
    }

    public ResourceAmounts minus(ResourceAmounts other) {
        if (other == null) {
            return this;
        }
        return new ResourceAmounts(
                emeralds - other.emeralds,
                ore - other.ore,
                crops - other.crops,
                fish - other.fish,
                wood - other.wood
        );
    }

    public long amount(ResourceKind kind) {
        return switch (kind) {
            case EMERALDS -> emeralds;
            case ORE -> ore;
            case CROPS -> crops;
            case FISH -> fish;
            case WOOD -> wood;
            case ALL -> total();
            case NONE -> 0L;
        };
    }

    public long materialTotal() {
        return ore + crops + fish + wood;
    }

    public long total() {
        return emeralds + materialTotal();
    }

    public boolean empty() {
        return total() <= 0L;
    }
}
