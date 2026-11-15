package com.yandere.mod;

import java.util.Random;

/** Yapay zeka kapalıyken ya da hata olunca kullanılan hazır replikler. */
public class Lines {
    private static final Random R = new Random();
    public static final String[] JOIN = {
        "Geldin mi canım? Seni çok bekledim... Hiç ayrılma tamam mı? ♥",
        "Oh, sonunda! Bu köyde kimse seni benim kadar sevmiyor, biliyorsun değil mi?",
        "Merhaba tatlım~ Bugün de sadece bana bakacaksın, değil mi?"
    };
    public static final String[] TALK = {
        "Seni dinliyorum canım... Hep dinliyorum. Her saniye. ♥",
        "Hmm? Bana mı söyledin? Başka kimseyle konuşmuyorsun, değil mi?",
        "Sesini duymak çok güzel... Biraz daha yaklaş, kimse duymasın.",
        "Bu kadar tatlı olmayı bırak, kendimi tutamıyorum~",
        "Sen sadece bana aitsin. Bunu biliyorsun, biliyorsun değil mi?"
    };
    public static final String[] CREEP = {
        "Seni görüyorum...", "Arkana bakma.", "Neden onlara bakıyorsun?", "Sadece benimsin.", "Hâlâ buradayım."
    };
    public static final String[] SEEN = {
        "Fark ettin mi? ...Fark ettin.", "Hep arkandaydım.", "Bir dahaki sefere bakma.", "Seni her gün izliyorum~"
    };
    public static final String[] PET = {
        "O hayvana bu kadar bakmana gerek yok...", "Neden ona sarılıyorsun? Ben buradayım.",
        "Ona verdiğin her dokunuş benden çalıntı.", "Hmm... onu da mı sevdin?"
    };
    public static final String[] MELTDOWN = {
        "NEDEN ONLARLA KONUŞUYORSUN",
        "S̷e̷n̷ ̷b̷a̷n̷a̷ ̷a̷i̷t̷s̷i̷n̷",
        "Bu köyde fazla insan var... Çok fazla...",
        "Onları temizleyeceğim. Hepsini. Sadece sen ve ben kalacağız."
    };
    public static final String[] CALM = {
        "Ay... ne yaptım ben? Özür dilerim canım, kendimi kaybettim. Kızmadın, değil mi? ♥",
        "Şşş, geçti. Hepsi geçti. Artık sadece biz varız, tatlım.",
        "Bazen sevgim taşıyor... Ama bu senin için, hep senin için. Sarılabilir miyim?"
    };
    public static String pick(String[] a) { return a[R.nextInt(a.length)]; }
}
