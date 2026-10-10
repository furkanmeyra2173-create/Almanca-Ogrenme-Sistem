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
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private ConnectivityManager connectivityManager;
    private TextView statusText;
    private TextView detailText;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 56, 40, 40);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(0xFF101820);

        TextView title = new TextView(this);
        title.setText("Ağ Yönetimi");
        title.setTextSize(28);
        title.setTextColor(0xFFFFFFFF);
        title.setGravity(Gravity.CENTER);
        root.addView(title, matchWrap());

        statusText = new TextView(this);
        statusText.setTextSize(21);
        statusText.setTextColor(0xFF80CBC4);
        statusText.setPadding(0, 36, 0, 12);
        statusText.setGravity(Gravity.CENTER);
        root.addView(statusText, matchWrap());

        detailText = new TextView(this);
        detailText.setTextColor(0xFFE0E0E0);
        detailText.setTextSize(16);
        detailText.setGravity(Gravity.CENTER);
        detailText.setPadding(0, 8, 0, 28);
        root.addView(detailText, matchWrap());

        Button refresh = new Button(this);
        refresh.setText("Bağlantıyı yenile");
        refresh.setOnClickListener(v -> refreshStatus());
        root.addView(refresh, matchWrap());

        Button start = new Button(this);
        start.setText("Arka plan takibini başlat");
        start.setOnClickListener(v -> startMonitor());
        root.addView(start, matchWrap());

        Button stop = new Button(this);
        stop.setText("Takibi durdur");
        stop.setOnClickListener(v -> {
            stopService(new Intent(this, NetworkMonitorService.class));
            refreshStatus();
        });
        root.addView(stop, matchWrap());

        TextView note = new TextView(this);
        note.setText("Bu sürüm bağlantı durumunu izler. VPN ile trafik yönlendirme henüz etkin değildir; internet bağlantınız değiştirilmez.");
        note.setTextColor(0xFFB0BEC5);
        note.setTextSize(13);
        note.setPadding(0, 28, 0, 0);
        root.addView(note, matchWrap());

        setContentView(root);
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 11);
        }
        refreshStatus();
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void startMonitor() {
        Intent intent = new Intent(this, NetworkMonitorService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent);
        else startService(intent);
        refreshStatus();
    }

    private void refreshStatus() {
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
