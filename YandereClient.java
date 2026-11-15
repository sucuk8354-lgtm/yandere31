package com.yandere.mod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class YandereClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        EntityRendererRegistry.register(YandereMod.YANDERE, YandereRenderer::new);
        EntityRendererRegistry.register(YandereMod.STALKER, StalkerRenderer::new);
    }
}
