package org.nia.niamod.models.api;

public record TerritoryResponse(Guild guild, Location location, String acquired) {
    public record Location(int[] start, int[] end) {
    }

    public record Guild(String name) {
    }
}