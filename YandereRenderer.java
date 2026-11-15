package com.yandere.mod;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;

public class YandereRenderer extends MobEntityRenderer<YandereEntity, PlayerEntityModel<YandereEntity>> {
    private static final Identifier SWEET = new Identifier(YandereMod.ID, "textures/entity/yandere_sweet.png");
    private static final Identifier PSYCHO = new Identifier(YandereMod.ID, "textures/entity/yandere_psycho.png");

    public YandereRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER_SLIM), true), 0.5f);
    }

    @Override public Identifier getTexture(YandereEntity e) { return e.isPsycho() ? PSYCHO : SWEET; }
}
