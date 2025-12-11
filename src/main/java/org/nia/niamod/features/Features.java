package org.nia.niamod.features;

public class Features {
    private static WebsocketFeature wsFeature;
    private static ResourceTickFeature resTickFeature;

    public static void init() {
        wsFeature = new WebsocketFeature();
        resTickFeature = new ResourceTickFeature();
    }

    public static WebsocketFeature getWsFeature() {
        return wsFeature;
    }

    public static ResourceTickFeature getResTickFeature() {
        return resTickFeature;
    }
}
