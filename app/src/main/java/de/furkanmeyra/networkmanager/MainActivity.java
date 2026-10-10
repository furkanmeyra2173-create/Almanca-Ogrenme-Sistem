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

import com.wireguard.config.Config;

import java.io.InputStream;

public class MainActivity extends Activity {
    private static final int VPN_REQUEST_CODE = 204;
    private static final int CONFIG_PICK_REQUEST_CODE = 205;
    private ConnectivityManager connectivityManager;
    private TextView statusText;
    private TextView typeText;
    private TextView detailsText;
    private TextView vpnStatusText;
    private TextView configText;
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
        subtitle.setText("Bağlantını ve kullanıcı onaylı WireGuard VPN'ini yönet.");
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

        content.addView(makeText("WireGuard VPN", 22, 0xFFFFFFFF, true), matchWrap());
        vpnStatusText = makeText(WireGuardController.getStatus(), 17, 0xFF80CBC4, true);
        content.addView(vpnStatusText, matchWrap());
        configText = makeText(WireGuardController.hasConfig()
            ? "WireGuard yapılandırması bu uygulama oturumunda yüklü."
            : "Henüz VPN sunucusu/yapılandırması eklenmedi.", 14, 0xFFE0E0E0, false);
        content.addView(configText, matchWrap());
        addButton("WireGuard .conf dosyası içe aktar", this::pickWireGuardConfig);
        addButton("WireGuard VPN'e bağlan", this::requestVpnStart);
        addButton("VPN bağlantısını kes", this::stopVpn);

        content.addView(makeText(
            "Full Tunnel desteği: sağlayıcının .conf dosyasındaki 0.0.0.0/0 ve ::/0 rotaları ile DNS ayarları desteklenir. Full Tunnel, internet trafiğini VPN sunucusuna yönlendirir; VPN bağlantısı kurulamazsa internet erişimi etkilenebilir. Yalnızca güvendiğin VPN sağlayıcısından aldığın yapılandırmayı içe aktar. Yapılandırma yalnızca bellekte tutulur; uygulama kapanınca yeniden içe aktarılmalıdır.",
            14, 0xFFFFCC80, false), matchWrap());

        refreshStatus();
        refreshVpnStatus();
    }

    private void pickWireGuardConfig() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, CONFIG_PICK_REQUEST_CODE);
    }

    private void requestVpnStart() {
        if (!WireGuardController.hasConfig()) {
            Toast.makeText(this, "Önce VPN sağlayıcından aldığın WireGuard .conf dosyasını içe aktar.", Toast.LENGTH_LONG).show();
            return;
        }
        if (WireGuardController.isConnected()) {
            refreshVpnStatus();
            return;
        }
        Intent consentIntent = VpnService.prepare(this);
        if (consentIntent != null) {
            startActivityForResult(consentIntent, VPN_REQUEST_CODE);
        } else {
            startWireGuard();
        }
    }

    private void startWireGuard() {
        vpnStatusText.setText("WireGuard bağlantısı kuruluyor…");
        WireGuardController.connect(getApplicationContext(), message -> runOnUiThread(() -> {
            if (vpnStatusText != null) vpnStatusText.setText(message);
            refreshStatus();
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }));
    }

    private void stopVpn() {
        WireGuardController.disconnect(message -> runOnUiThread(() -> {
            if (vpnStatusText != null) vpnStatusText.setText(message);
            refreshStatus();
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }));
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VPN_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                startWireGuard();
            } else {
                Toast.makeText(this, "VPN izni verilmedi; VPN başlatılmadı.", Toast.LENGTH_LONG).show();
            }
            refreshVpnStatus();
            return;
        }
        if (requestCode == CONFIG_PICK_REQUEST_CODE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            try (InputStream stream = getContentResolver().openInputStream(data.getData())) {
                if (stream == null) throw new IllegalArgumentException("Dosya okunamadı.");
                Config parsed = Config.parse(stream);
                WireGuardController.validateConfig(parsed);
                WireGuardController.setConfig(parsed);
                if (configText != null) configText.setText("WireGuard yapılandırması doğrulandı ve belleğe alındı.");
                refreshVpnStatus();
                Toast.makeText(this, "WireGuard yapılandırması yüklendi. Gizli anahtarlar kaydedilmedi.", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                String message = e.getMessage();
                Toast.makeText(this, "Yapılandırma reddedildi: " +
                    (message == null ? "dosya okunamadı veya biçim geçersiz." : message), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void refreshVpnStatus() {
        if (vpnStatusText == null) return;
        vpnStatusText.setText(WireGuardController.getStatus());
        if (configText != null) {
            configText.setText(WireGuardController.hasConfig()
                ? "WireGuard yapılandırması bu uygulama oturumunda yüklü."
                : "Henüz VPN sunucusu/yapılandırması eklenmedi.");
        }
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
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) type = "VPN";
            else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) type = "Wi-Fi";
            else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) type = "Mobil veri";
            else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) type = "Ethernet";
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
