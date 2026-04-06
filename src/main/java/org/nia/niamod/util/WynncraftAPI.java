package org.nia.niamod.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.experimental.UtilityClass;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.models.api.GuildResponse;
import org.nia.niamod.models.api.TerritoryResponse;

import java.lang.reflect.Type;
import java.util.Map;

@UtilityClass
public class WynncraftAPI {
    private static final Gson gson = new Gson();

    public static Map<String, TerritoryResponse> territoryResponse() {
        Type type = new TypeToken<Map<String, TerritoryResponse>>() {
        }.getType();
        return gson.fromJson(WebUtils.queryAPI(NyahConfig.nyahConfigData.getApiBase() + "guild/list/territory"), type);
    }

    public static GuildResponse guildResponse(String guildName) {
        return gson.fromJson(WebUtils.queryAPI(NyahConfig.nyahConfigData.getApiBase() + "guild/%s?identifier=username".formatted(guildName.replace(" ", "%20"))), GuildResponse.class);
    }
}
