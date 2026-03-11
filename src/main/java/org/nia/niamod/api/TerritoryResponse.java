package org.nia.niamod.api;

public class TerritoryResponse {
    public Location location;

    public TerritoryResponse(Location location) {
        this.location = location;
    }

    public static class Location {
        public int[] start;
        public int[] end;

        public Location(int[] start, int[] end) {
            this.start = start;
            this.end = end;
        }
    }
}