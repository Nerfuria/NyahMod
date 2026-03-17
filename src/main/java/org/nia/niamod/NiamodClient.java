package org.nia.niamod;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.features.Features;
import org.slf4j.Logger;

public class NiamodClient implements ClientModInitializer {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static MinecraftClient mc;

    private static final ThreadLocal<MatrixStack> NYAH_MATRIX = new ThreadLocal<>();

    public static MatrixStack getMatrixStack() {
        return NYAH_MATRIX.get();
    }

    public static  void setMatrixStack(MatrixStack matrixStack) {
        NYAH_MATRIX.set(matrixStack);
    }

    @Override
    public void onInitializeClient() {
        mc = MinecraftClient.getInstance();
        NyahConfig.init();
        Features.init();
    }
}
