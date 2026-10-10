package de.furkanmeyra.networkmanager;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.IBinder;

public class NetworkMonitorService extends Service {
    private static final String CHANNEL_ID = "network_monitor";
    private static final int NOTIFICATION_ID = 2071;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback callback;

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(NOTIFICATION_ID, makeNotification("Ağ durumu kontrol ediliyor"));
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        callback = new ConnectivityManager.NetworkCallback() {
            @Override public void onAvailable(Network network) {
                updateNotification("İnternet bağlantısı mevcut");
            }
            @Override public void onLost(Network network) {
                updateNotification("İnternet bağlantısı kesildi");
            }
            @Override public void onCapabilitiesChanged(Network network, NetworkCapabilities caps) {
                boolean internet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                updateNotification(internet ? "Ağ mevcut · internet özelliği algılandı" : "Ağ var, internet doğrulanamadı");
            }
        };
        try {
            connectivityManager.registerDefaultNetworkCallback(callback);
        } catch (RuntimeException ignored) {
            updateNotification("Ağ izleme başlatılamadı");
        }
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Ağ takibi", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Ağ bağlantısı durum bildirimleri");
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }
    }

    private Notification makeNotification(String text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Ağ Yönetimi")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setOngoing(true)
                .build();
        }
        return new Notification.Builder(this)
            .setContentTitle("Ağ Yönetimi")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setOngoing(true)
            .build();
    }

    private void updateNotification(String text) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID, makeNotification(text));
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override public void onDestroy() {
        if (connectivityManager != null && callback != null) {
            try { connectivityManager.unregisterNetworkCallback(callback); } catch (RuntimeException ignored) {}
        }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
