package com.yandere.mod;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;

public class StalkerRenderer extends MobEntityRenderer<StalkerEntity, PlayerEntityModel<StalkerEntity>> {
    private static final Identifier TEX = new Identifier(YandereMod.ID, "textures/entity/yandere_shadow.png");

    public StalkerRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER_SLIM), true), 0f);
    }

    @Override public Identifier getTexture(StalkerEntity e) { return TEX; }
}
