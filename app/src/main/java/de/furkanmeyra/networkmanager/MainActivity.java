package de.furkanmeyra.networkmanager;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private ConnectivityManager connectivityManager;
    private TextView statusText;
    private TextView detailText;
    private LinearLayout root;
    private LinearLayout content;
    private int gameQuestion = 0;
    private int score = 0;

    private final String[][] words = {
        {"das Haus", "ev"}, {"der Hund", "köpek"}, {"die Katze", "kedi"},
        {"das Wasser", "su"}, {"das Brot", "ekmek"}, {"die Schule", "okul"},
        {"das Buch", "kitap"}, {"die Familie", "aile"}, {"essen", "yemek yemek"},
        {"trinken", "içmek"}, {"lernen", "öğrenmek"}, {"sprechen", "konuşmak"}
    };
    private final String[] gameQuestions = {
        "„der Hund“ ne demek?", "„su“ Almanca nasıl söylenir?", "„Ich ___ müde.“ (geçmiş zaman)",
        "„kitap“ kelimesinin artikeli hangisidir?", "„hatte“ hangi fiilin geçmiş zamanıdır?"
    };
    private final String[][] gameOptions = {
        {"kedi", "köpek", "kuş"}, {"das Wasser", "das Brot", "das Haus"},
        {"bin", "war", "habe"}, {"das Buch", "die Buch", "der Buch"},
        {"sein", "haben", "gehen"}
    };
    private final int[] gameAnswers = {1, 0, 1, 0, 1};

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 28, 24, 24);
        root.setBackgroundColor(0xFF101820);

        TextView title = new TextView(this);
        title.setText("Almanca Öğren");
        title.setTextSize(28);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(0xFFFFFFFF);
        title.setGravity(Gravity.CENTER);
        root.addView(title, matchWrap());

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        String[] labels = {"Kelimeler", "Gramer", "Oyunlar", "Ağ"};
        String[] pages = {"words", "grammar", "games", "network"};
        for (int i = 0; i < labels.length; i++) {
            final String page = pages[i];
            Button b = new Button(this);
            b.setText(labels[i]);
            b.setTextSize(11);
            b.setOnClickListener(v -> showPage(page));
            nav.addView(b, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        }
        root.addView(nav, matchWrap());

        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(4, 20, 4, 8);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 11);
        }
        showPage("words");
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void showPage(String page) {
        content.removeAllViews();
        if ("words".equals(page)) showWords();
        else if ("grammar".equals(page)) showGrammar();
        else if ("games".equals(page)) showGames();
        else showNetwork();
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        t.setPadding(8, 10, 8, 10);
        return t;
    }

    private void addLine(String value, int size, boolean bold) {
        content.addView(text(value, size, 0xFFFFFFFF, bold), matchWrap());
    }

    private void showWords() {
        addLine("Kelime çalışması", 23, true);
        addLine("Almanca kelimeyi ve Türkçe anlamını birlikte öğren.", 15, false);
        for (String[] word : words) {
            TextView item = text(word[0] + "  —  " + word[1], 18, 0xFFB2DFDB, true);
            content.addView(item, matchWrap());
        }
    }

    private void showGrammar() {
        addLine("A2 Gramer: war ve hatte", 23, true);
        addLine("WAR — sein (olmak) fiilinin geçmiş zamanı", 18, true);
        addLine("ich war = ben idim\ndu warst = sen idin\ner/sie/es war = o idi\nwir waren = biz idik\nihr wart = siz idiniz\nsie/Sie waren = onlar / siz idiniz", 16, false);
        addLine("HATTE — haben (sahip olmak) fiilinin geçmiş zamanı", 18, true);
        addLine("ich hatte = benim vardı\ndu hattest = senin vardı\ner/sie/es hatte = onun vardı\nwir hatten = bizim vardı\nihr hattet = sizin vardı\nsie/Sie hatten = onların / sizin vardı", 16, false);
        addLine("Örnekler:", 18, true);
        addLine("Ich war gestern zu Hause. — Dün evdeydim.\nIch hatte keine Zeit. — Zamanım yoktu.", 16, false);
    }

    private void showGames() {
        addLine("Mini test", 23, true);
        addLine("Doğru cevabı seç. Her doğru cevap 1 puan.", 15, false);
        if (gameQuestion >= gameQuestions.length) {
            addLine("Test bitti! Puanın: " + score + " / " + gameQuestions.length, 22, true);
            Button again = new Button(this);
            again.setText("Yeniden oyna");
            again.setOnClickListener(v -> { gameQuestion = 0; score = 0; showGames(); });
            content.addView(again, matchWrap());
            return;
        }
        addLine("Soru " + (gameQuestion + 1) + "/" + gameQuestions.length, 15, false);
        addLine(gameQuestions[gameQuestion], 20, true);
        for (int i = 0; i < gameOptions[gameQuestion].length; i++) {
            final int chosen = i;
            Button answer = new Button(this);
            answer.setText(gameOptions[gameQuestion][i]);
            answer.setOnClickListener(v -> {
                if (chosen == gameAnswers[gameQuestion]) {
                    score++;
                    Toast.makeText(this, "Doğru cevap! 🎉", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Olmadı, bir sonrakinde!", Toast.LENGTH_SHORT).show();
                }
                gameQuestion++;
                showGames();
            });
            content.addView(answer, matchWrap());
        }
    }

    private void showNetwork() {
        addLine("Ağ durumu", 23, true);
        statusText = text("", 20, 0xFF80CBC4, true);
        content.addView(statusText, matchWrap());
        detailText = text("", 15, 0xFFE0E0E0, false);
        content.addView(detailText, matchWrap());

        Button refresh = new Button(this);
        refresh.setText("Bağlantıyı yenile");
        refresh.setOnClickListener(v -> refreshStatus());
        content.addView(refresh, matchWrap());

        Button start = new Button(this);
        start.setText("Arka plan takibini başlat");
        start.setOnClickListener(v -> startMonitor());
        content.addView(start, matchWrap());

        Button stop = new Button(this);
        stop.setText("Takibi durdur");
        stop.setOnClickListener(v -> {
            stopService(new Intent(this, NetworkMonitorService.class));
            refreshStatus();
        });
        content.addView(stop, matchWrap());
        addLine("Bu bölüm yalnızca bağlantı durumunu gösterir; internet trafiğini engellemez.", 13, false);
        refreshStatus();
    }

    private void startMonitor() {
        Intent intent = new Intent(this, NetworkMonitorService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent);
        else startService(intent);
        refreshStatus();
    }

    private void refreshStatus() {
        if (statusText == null || detailText == null) return;
        Network active = connectivityManager.getActiveNetwork();
        NetworkCapabilities caps = active == null ? null : connectivityManager.getNetworkCapabilities(active);
        boolean connected = caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        statusText.setText(connected ? "● İnternet bağlı" : "● İnternet bağlı değil");
        String type = "Bağlantı türü: bilinmiyor";
        if (caps != null) {
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) type = "Bağlantı türü: Wi‑Fi";
            else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) type = "Bağlantı türü: mobil veri";
            else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) type = "Bağlantı türü: Ethernet";
            else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) type = "Bağlantı türü: VPN";
        }
        detailText.setText(type + "\nArka plan takibini düğmeyle başlatabilirsin.");
    }

    @Override protected void onResume() {
        super.onResume();
        if (statusText != null) refreshStatus();
    }
}
