package com.yandere.mod;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class YandereMod implements ModInitializer {
    public static final String ID = "yanderemod";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);
    public static EntityType<YandereEntity> YANDERE;
    public static EntityType<StalkerEntity> STALKER;

    @Override
    public void onInitialize() {
        Config.load();
        YANDERE = Registry.register(Registries.ENTITY_TYPE, new Identifier(ID, "yandere"),
                FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, YandereEntity::new)
                        .dimensions(EntityDimensions.fixed(0.6f, 1.8f)).build());
        FabricDefaultAttributeRegistry.register(YANDERE, YandereEntity.createAttrs());
        STALKER = Registry.register(Registries.ENTITY_TYPE, new Identifier(ID, "stalker"),
                FabricEntityTypeBuilder.create(SpawnGroup.MISC, StalkerEntity::new)
                        .dimensions(EntityDimensions.fixed(0.6f, 1.8f)).build());
        FabricDefaultAttributeRegistry.register(STALKER, StalkerEntity.createAttrs());

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                server.execute(() -> {
                    ensureSpawned(handler.player);
                    ChatAI.say(handler.player, Lines.pick(Lines.JOIN));
                }));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> Memory.save(handler.player));

        // Her 5 saniyede bir kız yoksa (boyut değişimi vb.) yeniden doğur
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 100 != 0) return;
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) ensureSpawned(p);
        });

        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            YandereEntity y = find(sender);
            if (y != null) ChatAI.ask(sender, y, message.getSignedContent());
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) ->
            dispatcher.register(literal("yandere").requires(s -> s.hasPermissionLevel(2))
                .then(literal("spawn").executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                    ensureSpawned(p); return 1; }))
                .then(literal("meltdown").executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                    YandereEntity y = find(p); if (y != null) y.startMeltdown(p); return 1; }))
                .then(literal("stalker").executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                    YandereEntity y = find(p); if (y != null) y.trySpawnStalker(p); return 1; }))
                .then(literal("calm").executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                    YandereEntity y = find(p); if (y != null) y.calm(p); return 1; }))
                .then(literal("jealousy").then(argument("n", IntegerArgumentType.integer(0, 100)).executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                    YandereEntity y = find(p);
                    if (y != null) y.jealousy = IntegerArgumentType.getInteger(ctx, "n");
                    return 1; })))
                .then(literal("status").executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                    YandereEntity y = find(p);
                    ctx.getSource().sendFeedback(() -> Text.literal(y == null ? "Yok" :
                        "Kıskançlık: " + Math.round(y.jealousy) + " | Psikopat: " + y.isPsycho() + " | Öldürülen: " + Memory.get(p).villagersKilled), false);
                    return 1; }))
            ));
    }

    public static YandereEntity find(ServerPlayerEntity player) {
        ServerWorld w = (ServerWorld) player.getWorld();
        List<YandereEntity> l = w.getEntitiesByType(YANDERE, e -> player.getUuid().equals(e.getOwnerId()));
        return l.isEmpty() ? null : l.get(0);
    }

    public static void ensureSpawned(ServerPlayerEntity player) {
        if (find(player) != null) return;
        ServerWorld w = (ServerWorld) player.getWorld();
        YandereEntity y = YANDERE.create(w);
        if (y == null) return;
        y.setOwnerId(player.getUuid());
        y.refreshPositionAndAngles(player.getX() + 1.5, player.getY(), player.getZ() + 1.5, player.getYaw(), 0);
        w.spawnEntity(y);
    }
}
