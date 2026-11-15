package com.yandere.mod;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** Ayarlar: config/yanderehorror.json. API anahtarı boşsa ANTHROPIC_API_KEY ortam değişkenine bakılır. */
public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static Config I = new Config();

    public String characterName = "Aiko";
    public boolean enableAi = true;
    public String apiKey = "";
    public String model = "claude-opus-5-5";   // ucuz: claude-sonnet-5-5 / claude-haiku-5-5
    public int maxTokens = 300;
    public int historyTurns = 20;
    public boolean enableVillagerKills = true;
    public double jealousyPerVillagerPerSecond = 0.4;
    public double jealousyDecayPerSecond = 0.15;
    public double meltdownThreshold = 100.0;
    /** Sesli sohbet köprüsü (yandere-ses.html sayfası buraya bağlanır). */
    public boolean enableVoiceBridge = true;
    public int voicePort = 8765;

    static Path path() { return FabricLoader.getInstance().getConfigDir().resolve("yanderehorror.json"); }

    public static void load() {
        try {
            Path p = path();
            if (Files.exists(p)) {
                Config c = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), Config.class);
                if (c != null) I = c;
            }
            Files.writeString(p, GSON.toJson(I), StandardCharsets.UTF_8);
        } catch (Exception e) { YandereMod.LOGGER.error("Config hatası: {}", e.toString()); }
    }

    public static String apiKey() {
        String k = I.apiKey;
        if (k == null || k.isBlank()) k = System.getenv("ANTHROPIC_API_KEY");
        return k == null ? "" : k.trim();
    }

    public static String name() {
        return (I.characterName == null || I.characterName.isBlank()) ? "Aiko" : I.characterName.trim();
    }
}
