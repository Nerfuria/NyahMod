package org.nia.niamod.features;

public class Features {
    private static WebsocketFeature wsFeature;

    public static void init() {
        wsFeature = new WebsocketFeature();
    }

    public static WebsocketFeature getWsFeature() {
        return wsFeature;
    }
}
