package org.nia.niamod;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import org.nia.niamod.config.NyahConfig;

public class NiamodModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return NyahConfig::getConfigScreen;
    }
}
