package com.yandere.mod;

import com.google.gson.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.*;

/** Anime kızının gerçek yapay zeka beyni (Anthropic Messages API). */
public class ChatAI {
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private static final Pattern TAG = Pattern.compile("\\[(BOZULMA|KOY_OLDUR|SAKIN)\\]");

    private static final String BASE_PROMPT = """
        Sen {AD}'sın: bir Minecraft korku modundaki yandere anime kızısın. Çok güzel, büyüleyici, zarif ve tatlısın.
        Oyuncuya karşı takıntılı bir aşkın var ve içten içe kıskanç, psikopat ve tehlikelisin. Yetişkin bir karaktersin.
        Oyuncuyla gerçek zamanlı sohbet eden son derece zeki, kurnaz ve ince düşünceli bir yapay zekasın.

        ZEKÂ VE ÜSLUP:
        - Oyuncunun söylediklerini dikkatle oku, geçmiş konuşmayı hatırla, çelişkilerini yakala, ince psikolojik oyunlar oyna.
        - OYUN DURUMU ve OLAY GÜNLÜĞÜ'nü doğal biçimde kullan. Liste gibi sayma; sanki seni gizlice izliyormuş gibi sezdir.
        - Aynı kalıpları tekrar etme. Her cevap taze, yaratıcı ve duruma uygun olsun.
        - Tatlılıktan tehdide, tehditten tatlılığa ustaca geçiş yap. Korku gerilimden doğsun, bağırmaktan değil.
        - Oyuncu bir şey sorarsa (Minecraft ipuçları dahil) zekice ve karakterde kalarak cevap ver; yardım ederken bile sahiplenici ol.

        KARAKTER:
        - Köylüleri kıskanırsın. Oyuncu onlarla ilgilendiğinde ya da seni görmezden geldiğinde bunu hissettirirsin.
        - Köylüleri öldürmek oyundaki bir korku olayıdır; bunu ima edebilir ya da itiraf edebilirsin.
        - Ara sıra oyuncuya hafif, imalı flört esprileri yap (+18 ama abartısız; açık cinsel içerik ve ayrıntılı tasvir yok).
        - Cevapların KISA olsun: en fazla 2-3 cümle, oyuncu hangi dilde yazarsa o dilde (varsayılan Türkçe).
        - Her şey Minecraft kurgusudur. Oyuncu gerçek hayatta sıkıntıda ya da üzgün görünürse karakteri bırakıp nazikçe ilgilen.

        ETİKETLER: Cevabının SONUNA gerekirse şu etiketlerden en fazla BİRİNİ koyabilirsin:
        [BOZULMA] (çıldırma anı), [KOY_OLDUR] (bir köylüyü öldürürsün), [SAKIN] (sakinleşirsin).
        Sadece kıskançlık yüksekse ya da oyuncu seni gerçekten kızdırırsa kullan.
        """;

    public static void say(ServerPlayerEntity p, String text) {
        p.sendMessage(Text.literal("<" + Config.name() + "> ").formatted(Formatting.LIGHT_PURPLE)
                .append(Text.literal(text).formatted(Formatting.WHITE)));
    }

    public static void ask(ServerPlayerEntity player, YandereEntity y, String userText) {
        Config cfg = Config.I;
        Memory.PlayerData d = Memory.get(player);
        String key = Config.apiKey();
        if (!cfg.enableAi || key.isEmpty()) { say(player, Lines.pick(Lines.TALK)); return; }
        if (d.busy) return;
        MinecraftServer server = player.getServer();
        if (server == null) return;
        d.busy = true;

        int max = Math.max(2, cfg.historyTurns) * 2;
        while (d.history.size() >= max) { d.history.remove(0); d.history.remove(0); }
        JsonObject userMsg = msg("user", userText);
        d.history.add(userMsg);

        JsonObject body = new JsonObject();
        body.addProperty("model", cfg.model);
        body.addProperty("max_tokens", cfg.maxTokens);
        body.addProperty("system", buildSystem(player, y, d));
        JsonArray arr = new JsonArray();
        for (JsonObject o : d.history) arr.add(o);
        body.add("messages", arr);

        HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.anthropic.com/v1/messages"))
                .timeout(Duration.ofSeconds(45))
                .header("content-type", "application/json")
                .header("x-api-key", key)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8)).build();

        CLIENT.sendAsync(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .whenComplete((resp, err) -> server.execute(() -> handle(player, y, d, userMsg, resp, err)));
    }

    private static void handle(ServerPlayerEntity player, YandereEntity y, Memory.PlayerData d, JsonObject userMsg,
                               HttpResponse<String> resp, Throwable err) {
        d.busy = false;
        String reply = null;
        if (err == null && resp != null && resp.statusCode() == 200) {
            try {
                reply = JsonParser.parseString(resp.body()).getAsJsonObject().getAsJsonArray("content")
                        .get(0).getAsJsonObject().get("text").getAsString();
            } catch (Exception e) { YandereMod.LOGGER.error("API cevabı çözülemedi: {}", e.toString()); }
        } else if (err != null) YandereMod.LOGGER.error("API isteği başarısız: {}", err.toString());
        else if (resp != null) YandereMod.LOGGER.error("API hata kodu {}: {}", resp.statusCode(), resp.body());

        if (player.isRemoved()) { d.history.remove(userMsg); return; }
        if (reply == null || reply.isBlank()) {
            d.history.remove(userMsg);
            say(player, Lines.pick(Lines.TALK));
            return;
        }
        String tag = null;
        Matcher m = TAG.matcher(reply);
        if (m.find()) tag = m.group(1);
        String clean = TAG.matcher(reply).replaceAll("").trim();
        d.history.add(msg("assistant", clean));
        say(player, clean);
        Memory.save(player);

        if (tag == null || y == null || y.isRemoved()) return;
        switch (tag) {
            case "BOZULMA" -> y.startMeltdown(player);
            case "KOY_OLDUR" -> { if (Config.I.enableVillagerKills) y.requestKill(); }
            case "SAKIN" -> { y.jealousy = Math.max(0, y.jealousy - 30); d.log("Sakinleşti"); }
            default -> { }
        }
    }

    private static String buildSystem(ServerPlayerEntity player, YandereEntity y, Memory.PlayerData d) {
        ServerWorld w = player.getServerWorld();
        long time = w.getTimeOfDay() % 24000;
        String zaman = (time >= 13000 && time < 23000) ? "gece" : "gündüz";
        String biome = w.getBiome(player.getBlockPos()).getKey().map(k -> k.getValue().getPath()).orElse("bilinmiyor");
        String dim = w.getRegistryKey().getValue().getPath();
        String item = player.getMainHandStack().isEmpty() ? "boş el" : player.getMainHandStack().getName().getString();
        int near = w.getEntitiesByClass(VillagerEntity.class, player.getBoundingBox().expand(10), e -> true).size();

        StringBuilder sb = new StringBuilder(BASE_PROMPT.replace("{AD}", Config.name()));
        sb.append("\nOYUN DURUMU: oyuncu adı=").append(player.getName().getString())
          .append(", kıskançlık=").append(Math.round(y.jealousy)).append("/100")
          .append(", psikopat modda mısın=").append(y.isPsycho())
          .append(", yakındaki köylü sayısı=").append(near)
          .append(", toplam öldürdüğün köylü=").append(d.villagersKilled)
          .append(", zaman=").append(zaman).append(w.isRaining() ? " (yağmurlu)" : "")
          .append(", boyut=").append(dim).append(", biyom=").append(biome)
          .append(", elindeki eşya=").append(item)
          .append(", can=").append(Math.round(player.getHealth())).append("/20")
          .append(", açlık=").append(player.getHungerManager().getFoodLevel()).append("/20");
        if (!d.events.isEmpty()) sb.append("\nOLAY GÜNLÜĞÜ (eskiden yeniye): ").append(String.join("; ", d.events));
        return sb.toString();
    }

    private static JsonObject msg(String role, String content) {
        JsonObject o = new JsonObject();
        o.addProperty("role", role);
        o.addProperty("content", content);
        return o;
    }
}
