package org.nia.niamod.models.api;

public record TerritoryResponse(Location location) {
    public record Location(int[] start, int[] end) {
    }
}