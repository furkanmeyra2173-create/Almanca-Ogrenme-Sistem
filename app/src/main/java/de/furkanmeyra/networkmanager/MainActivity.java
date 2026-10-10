package de.furkanmeyra.networkmanager;

import android.app.Activity;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private ConnectivityManager connectivityManager;
    private TextView statusText;
    private TextView typeText;
    private TextView detailsText;
    private LinearLayout content;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 28, 24, 24);
        root.setBackgroundColor(0xFF101820);

        TextView title = new TextView(this);
        title.setText("Ağ Yönetimi");
        title.setTextSize(28);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(0xFFFFFFFF);
        title.setGravity(Gravity.CENTER);
        root.addView(title, matchWrap());

        TextView subtitle = new TextView(this);
        subtitle.setText("Bağlantını görüntüle ve arka plan takibini kendin yönet.");
        subtitle.setTextSize(15);
        subtitle.setTextColor(0xFFCFD8DC);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(8, 12, 8, 20);
        root.addView(subtitle, matchWrap());

        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(4, 8, 4, 8);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);

        showDashboard();
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private TextView makeText(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        t.setPadding(8, 12, 8, 12);
        return t;
    }

    private void showDashboard() {
        content.removeAllViews();

        content.addView(makeText("Bağlantı durumu", 22, 0xFFFFFFFF, true), matchWrap());
        statusText = makeText("Durum kontrol ediliyor…", 20, 0xFF80CBC4, true);
        content.addView(statusText, matchWrap());
        typeText = makeText("Bağlantı türü: —", 17, 0xFFE0E0E0, false);
        content.addView(typeText, matchWrap());
        detailsText = makeText("Ayrıntılar burada gösterilir.", 14, 0xFFB0BEC5, false);
        content.addView(detailsText, matchWrap());

        Button refresh = new Button(this);
        refresh.setText("Bağlantıyı yenile");
        refresh.setOnClickListener(v -> refreshStatus());
        content.addView(refresh, matchWrap());

        content.addView(makeText("Arka plan takibi", 22, 0xFFFFFFFF, true), matchWrap());

        Button start = new Button(this);
        start.setText("Takibi başlat");
        start.setOnClickListener(v -> {
            Intent intent = new Intent(this, NetworkMonitorService.class);
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent);
                else startService(intent);
                Toast.makeText(this, "Ağ takibi başlatma isteği gönderildi.", Toast.LENGTH_SHORT).show();
            } catch (RuntimeException e) {
                Toast.makeText(this, "Takip başlatılamadı. Bildirim/izin ayarlarını kontrol et.", Toast.LENGTH_LONG).show();
            }
            refreshStatus();
        });
        content.addView(start, matchWrap());

        Button stop = new Button(this);
        stop.setText("Takibi durdur");
        stop.setOnClickListener(v -> {
            stopService(new Intent(this, NetworkMonitorService.class));
            Toast.makeText(this, "Ağ takibi durduruldu.", Toast.LENGTH_SHORT).show();
        });
        content.addView(stop, matchWrap());

        content.addView(makeText(
            "Bilgi: Bu sürüm yalnızca ağ bağlantısının durumunu izler. İnternet trafiğini engellemez, diğer uygulamaların bağlantılarını değiştirmez ve uygulamayı gizlemez. Takip başlatıldığında Android, devam eden hizmet için bildirim gösterebilir.",
            14, 0xFFFFCC80, false), matchWrap());

        refreshStatus();
    }

    private void refreshStatus() {
        if (statusText == null || typeText == null || detailsText == null) return;
        Network active = connectivityManager.getActiveNetwork();
        NetworkCapabilities caps = active == null ? null : connectivityManager.getNetworkCapabilities(active);
        boolean connected = caps != null &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        statusText.setText(connected ? "● İnternet ağı algılandı" : "● İnternet ağı algılanmadı");

        String type = "Bilinmiyor";
        if (caps != null) {
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) type = "Wi‑Fi";
            else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) type = "Mobil veri";
            else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) type = "Ethernet";
            else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) type = "VPN";
        }
        typeText.setText("Bağlantı türü: " + type);
        detailsText.setText(connected
            ? "Android etkin bir ağ ve internet yeteneği bildiriyor. Bu, her internet sitesinin erişilebilir olduğunu garanti etmez."
            : "Şu anda Android tarafından internet özelliğine sahip etkin bir ağ bildirilmedi.");
    }

    @Override protected void onResume() {
        super.onResume();
        refreshStatus();
    }
}
