package com.yandere.mod;

import com.google.gson.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.*;

/**
 * Sesli sohbet köprüsü. Sadece bu bilgisayardan (127.0.0.1) erişilir.
 * yandere-ses.html sayfası konuştuğunu yazıya çevirip buraya yollar, kızın cevabını geri alıp sesli okur.
 */
public class VoiceServer {
    private static ServerSocket sock;
    private static final ExecutorService POOL = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "yandere-voice-worker");
        t.setDaemon(true);
        return t;
    });

    public static void start(MinecraftServer server) {
        stop();
        if (!Config.I.enableVoiceBridge) return;
        try {
            sock = new ServerSocket(Config.I.voicePort, 20, InetAddress.getLoopbackAddress());
            final ServerSocket s = sock;
            Thread t = new Thread(() -> {
                while (!s.isClosed()) {
                    try {
                        Socket c = s.accept();
                        POOL.submit(() -> handle(server, c));
                    } catch (IOException e) { break; }
                }
            }, "yandere-voice");
            t.setDaemon(true);
            t.start();
            YandereMod.LOGGER.info("Sesli sohbet köprüsü açık: http://127.0.0.1:{}", Config.I.voicePort);
        } catch (Exception e) {
            YandereMod.LOGGER.error("Sesli sohbet köprüsü açılamadı: {}", e.toString());
        }
    }

    public static void stop() {
        try { if (sock != null) sock.close(); } catch (IOException ignored) { }
        sock = null;
    }

    private static void handle(MinecraftServer server, Socket c) {
        try (c) {
            c.setSoTimeout(70000);
            InputStream in = new BufferedInputStream(c.getInputStream());
            OutputStream out = c.getOutputStream();

            String reqLine = readLine(in);
            if (reqLine == null || reqLine.isEmpty()) return;
            String[] parts = reqLine.split(" ");
            String method = parts[0];
            String path = parts.length > 1 ? parts[1] : "/";
            int len = 0;
            String line;
            while ((line = readLine(in)) != null && !line.isEmpty()) {
                int i = line.indexOf(':');
                if (i > 0 && line.substring(0, i).trim().equalsIgnoreCase("content-length"))
                    len = Integer.parseInt(line.substring(i + 1).trim());
            }
            byte[] body = new byte[Math.min(len, 20000)];
            int read = 0;
            while (read < body.length) {
                int r = in.read(body, read, body.length - read);
                if (r < 0) break;
                read += r;
            }

            if ("OPTIONS".equals(method)) { send(out, 204, ""); return; }
            if (path.startsWith("/ping")) { send(out, 200, "{\"ok\":true}"); return; }
            if (!path.startsWith("/say") || !"POST".equals(method)) { send(out, 404, "{\"error\":\"yok\"}"); return; }

            String text = JsonParser.parseString(new String(body, 0, read, StandardCharsets.UTF_8))
                    .getAsJsonObject().get("text").getAsString().trim();
            if (text.isEmpty()) { send(out, 400, "{\"error\":\"bos\"}"); return; }

            CompletableFuture<String> fut = new CompletableFuture<>();
            server.execute(() -> {
                List<ServerPlayerEntity> ps = server.getPlayerManager().getPlayerList();
                if (ps.isEmpty()) { fut.complete(json("", false)); return; }
                ServerPlayerEntity p = ps.get(0);
                YandereEntity y = YandereMod.find(p);
                if (y == null) { fut.complete(json("", false)); return; }
                p.sendMessage(Text.literal("§7[Ses] " + p.getName().getString() + ": " + text));
                ChatAI.ask(p, y, text, reply -> fut.complete(json(reply, y.isPsycho())));
            });
            send(out, 200, fut.get(60, TimeUnit.SECONDS));
        } catch (Exception e) {
            try { send(c.getOutputStream(), 500, "{\"error\":\"hata\"}"); } catch (Exception ignored) { }
        }
    }

    private static String json(String reply, boolean psycho) {
        JsonObject o = new JsonObject();
        o.addProperty("reply", reply);
        o.addProperty("psycho", psycho);
        return o.toString();
    }

    private static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        int ch;
        while ((ch = in.read()) != -1) {
            if (ch == '\n') break;
            if (ch != '\r') b.write(ch);
        }
        if (ch == -1 && b.size() == 0) return null;
        return b.toString(StandardCharsets.ISO_8859_1);
    }

    private static void send(OutputStream out, int code, String body) throws IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        String head = "HTTP/1.1 " + code + " OK\r\n"
                + "Content-Type: application/json; charset=utf-8\r\n"
                + "Content-Length: " + b.length + "\r\n"
                + "Access-Control-Allow-Origin: *\r\n"
                + "Access-Control-Allow-Headers: *\r\n"
                + "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n"
                + "Access-Control-Allow-Private-Network: true\r\n"
                + "Connection: close\r\n\r\n";
        out.write(head.getBytes(StandardCharsets.ISO_8859_1));
        out.write(b);
        out.flush();
    }
}
