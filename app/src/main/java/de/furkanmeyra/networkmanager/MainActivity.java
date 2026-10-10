package de.furkanmeyra.networkmanager;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int VPN_REQUEST_CODE = 204;
    private ConnectivityManager connectivityManager;
    private TextView statusText;
    private TextView typeText;
    private TextView detailsText;
    private TextView vpnStatusText;
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
        subtitle.setText("Bağlantını ve kullanıcı onaylı VPN servisini yönet.");
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

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 11);
        }
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

    private void addButton(String label, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setOnClickListener(v -> action.run());
        content.addView(button, matchWrap());
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
        addButton("Bağlantıyı yenile", this::refreshStatus);

        content.addView(makeText("Arka plan takibi", 22, 0xFFFFFFFF, true), matchWrap());
        addButton("Takibi başlat", () -> {
            Intent intent = new Intent(this, NetworkMonitorService.class);
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent);
                else startService(intent);
                Toast.makeText(this, "Ağ takibi başlatma isteği gönderildi.", Toast.LENGTH_SHORT).show();
            } catch (RuntimeException e) {
                Toast.makeText(this, "Takip başlatılamadı. Bildirim/izin ayarlarını kontrol et.", Toast.LENGTH_LONG).show();
            }
        });
        addButton("Takibi durdur", () -> {
            stopService(new Intent(this, NetworkMonitorService.class));
            Toast.makeText(this, "Ağ takibi durduruldu.", Toast.LENGTH_SHORT).show();
        });

        content.addView(makeText("VPN kontrolü", 22, 0xFFFFFFFF, true), matchWrap());
        vpnStatusText = makeText("VPN durumu kontrol ediliyor…", 18, 0xFF80CBC4, true);
        content.addView(vpnStatusText, matchWrap());
        addButton("VPN'i aç", this::requestVpnStart);
        addButton("VPN'i kapat", this::stopVpn);

        content.addView(makeText(
            "VPN altyapısı ilk aşamadadır. Şu an kullanıcı onayıyla bir VPN arayüzü oluşturur ancak trafik rotası/DNS eklemez ve internet trafiğini yakalamaz ya da engellemez. Gerçek trafik yönlendirmesi ayrı bir sonraki geliştirmedir. Android, VPN çalışırken durum bildirimi gösterebilir.",
            14, 0xFFFFCC80, false), matchWrap());

        refreshStatus();
        refreshVpnStatus();
    }

    private void requestVpnStart() {
        if (VpnControlService.running) {
            Toast.makeText(this, "VPN zaten çalışıyor.", Toast.LENGTH_SHORT).show();
            refreshVpnStatus();
            return;
        }
        Intent consentIntent = VpnService.prepare(this);
        if (consentIntent != null) {
            startActivityForResult(consentIntent, VPN_REQUEST_CODE);
        } else {
            startVpnService();
        }
    }

    private void startVpnService() {
        try {
            Intent intent = new Intent(this, VpnControlService.class);
            intent.setAction(VpnControlService.ACTION_START);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent);
            else startService(intent);
            Toast.makeText(this, "VPN başlatma isteği gönderildi.", Toast.LENGTH_SHORT).show();
        } catch (RuntimeException e) {
            Toast.makeText(this, "VPN başlatılamadı. İzin ve sistem ayarlarını kontrol et.", Toast.LENGTH_LONG).show();
        }
        refreshVpnStatus();
    }

    private void stopVpn() {
        Intent intent = new Intent(this, VpnControlService.class);
        intent.setAction(VpnControlService.ACTION_STOP);
        try {
            startService(intent);
        } catch (RuntimeException ignored) {
            stopService(new Intent(this, VpnControlService.class));
        }
        VpnControlService.running = false;
        Toast.makeText(this, "VPN kapatma isteği gönderildi.", Toast.LENGTH_SHORT).show();
        refreshVpnStatus();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VPN_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                startVpnService();
            } else {
                Toast.makeText(this, "VPN izni verilmedi; VPN başlatılmadı.", Toast.LENGTH_LONG).show();
            }
            refreshVpnStatus();
        }
    }

    private void refreshVpnStatus() {
        if (vpnStatusText == null) return;
        vpnStatusText.setText(VpnControlService.running
            ? "● VPN altyapısı açık (trafik yönlendirmesi kapalı)"
            : "● VPN kapalı");
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
        refreshVpnStatus();
    }
}
