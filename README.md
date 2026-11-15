# Yandere Horror (Fabric 1.20.1)
1. JDK 17 + IntelliJ kur.
2. https://fabricmc.net/develop/template/ adresinden 1.20.1 şablonu indir, `gradle/wrapper`, `gradlew`, `gradlew.bat` dosyalarını bu klasöre kopyala.
3. `./gradlew build` -> `build/libs/yanderemod-0.1.0.jar` dosyasını `mods/` klasörüne at (Fabric Loader + Fabric API şart).
4. İlk açılışta `config/yanderehorror.json` oluşur. `apiKey` alanına Claude API anahtarını yaz ya da ANTHROPIC_API_KEY ortam değişkenini kullan. Anahtar yoksa hazır replikler çalışır.
5. Komutlar (op): /yandere spawn | status | jealousy <0-100> | meltdown | calm

Hafıza: config/yandere-memory/<uuid>.json (sohbet geçmişi + olay günlüğü, oyun kapanınca da saklanır).
Ayarlar: config/yanderehorror.json (model, kıskançlık hızı, köylü öldürme aç/kapa...).

## Jar'ı bilgisayara hiçbir şey kurmadan derlemek (GitHub)
1. github.com'da ücretsiz hesap aç, yeni bir repo oluştur.
2. Bu klasörün İÇİNDEKİ her şeyi (src, build.gradle, .github dahil) repoya yükle.
3. Repoda "Actions" sekmesi > "Build mod" > çalışmasını bekle (2-5 dk).
4. Bitince çalışmanın sayfasındaki "yandere-mod-jar" dosyasını indir, içinden çıkan
   `yanderemod-0.1.0.jar` dosyasını mods/ klasörüne at (Fabric Loader + Fabric API gerekir).
Not: `-sources.jar` ile biten dosyayı kullanma.
