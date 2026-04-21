package org.nia.niamod.models.gui.territory;

public record Resources(int emeralds, int ore, int crops, int fish, int wood) {
    public static final Resources EMPTY = new Resources(0, 0, 0, 0, 0);

    public int materialTotal() {
        return ore + crops + fish + wood;
    }

    public ResourceKind kind() {
        boolean hasCrops = crops > 0;
        boolean hasWood = wood > 0;
        boolean hasOre = ore > 0;
        boolean hasFish = fish > 0;
        if (hasCrops && hasWood && hasOre && hasFish) {
            return ResourceKind.ALL;
        }

        int max = Math.max(Math.max(crops, wood), Math.max(ore, fish));
        if (max <= 0) {
            return ResourceKind.NONE;
        }
        if (crops == max) {
            return ResourceKind.CROPS;
        }
        if (wood == max) {
            return ResourceKind.WOOD;
        }
        if (ore == max) {
            return ResourceKind.ORE;
        }
        return ResourceKind.FISH;
    }
}
