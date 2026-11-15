package com.yandere.mod;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Oyuncu başına kalıcı hafıza: config/yandere-memory/<uuid>.json. Oyun kapanınca da seni hatırlar. */
public class Memory {
    public static class PlayerData {
        public int villagersKilled = 0;
        public boolean busy = false;
        public final List<JsonObject> history = new ArrayList<>();
        public final List<String> events = new ArrayList<>();
        public void log(String e) { events.add(e); while (events.size() > 12) events.remove(0); }
    }

    private static final Gson GSON = new Gson();
    private static final Map<UUID, PlayerData> DATA = new HashMap<>();

    public static PlayerData get(ServerPlayerEntity p) { return DATA.computeIfAbsent(p.getUuid(), Memory::load); }

    private static Path file(UUID id) {
        return FabricLoader.getInstance().getConfigDir().resolve("yandere-memory").resolve(id + ".json");
    }

    private static PlayerData load(UUID id) {
        PlayerData d = new PlayerData();
        try {
            Path p = file(id);
            if (Files.exists(p)) {
                JsonObject r = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
                if (r.has("villagersKilled")) d.villagersKilled = r.get("villagersKilled").getAsInt();
                if (r.has("history")) {
                    for (JsonElement e : r.getAsJsonArray("history")) d.history.add(e.getAsJsonObject());
                    while (!d.history.isEmpty() && (d.history.size() % 2 != 0
                            || !"user".equals(d.history.get(0).get("role").getAsString()))) d.history.remove(0);
                }
                if (r.has("events")) for (JsonElement e : r.getAsJsonArray("events")) d.events.add(e.getAsString());
            }
        } catch (Exception e) { YandereMod.LOGGER.error("Hafıza okunamadı: {}", e.toString()); }
        return d;
    }

    public static void save(ServerPlayerEntity player) {
        PlayerData d = DATA.get(player.getUuid());
        if (d == null) return;
        try {
            JsonObject r = new JsonObject();
            r.addProperty("villagersKilled", d.villagersKilled);
            JsonArray h = new JsonArray();
            int n = d.history.size() - (d.history.size() % 2);
            for (int i = 0; i < n; i++) h.add(d.history.get(i));
            r.add("history", h);
            JsonArray ev = new JsonArray();
            for (String s : d.events) ev.add(s);
            r.add("events", ev);
            Path p = file(player.getUuid());
            Files.createDirectories(p.getParent());
            Files.writeString(p, GSON.toJson(r), StandardCharsets.UTF_8);
        } catch (Exception e) { YandereMod.LOGGER.error("Hafıza kaydedilemedi: {}", e.toString()); }
    }
}
