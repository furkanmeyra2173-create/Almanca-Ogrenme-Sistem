package de.furkanmeyra.networkmanager;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;

public class VpnControlService extends VpnService {
    public static final String ACTION_START = "de.furkanmeyra.networkmanager.VPN_START";
    public static final String ACTION_STOP = "de.furkanmeyra.networkmanager.VPN_STOP";
    public static volatile boolean running = false;

    private static final String CHANNEL_ID = "vpn_status_channel";
    private static final int NOTIFICATION_ID = 2072;
    private ParcelFileDescriptor vpnInterface;

    @Override public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, makeNotification("VPN hazırlanıyor"));
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_START : intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopVpn();
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        startVpnSafely();
        return START_NOT_STICKY;
    }

    private void startVpnSafely() {
        if (running && vpnInterface != null) {
            updateNotification("VPN çalışıyor · yönlendirme kapalı");
            return;
        }
        try {
            Builder builder = new Builder()
                .setSession("Ağ Yönetimi VPN")
                .setMtu(1500)
                .addAddress("10.10.10.2", 32);
            // Deliberately no routes or DNS: this initial skeleton does not
            // capture or interrupt device traffic until routing is implemented.
            vpnInterface = builder.establish();
            running = vpnInterface != null;
            updateNotification(running
                ? "VPN altyapısı etkin · trafik yönlendirmesi henüz kapalı"
                : "VPN başlatılamadı");
            if (!running) {
                stopForeground(true);
                stopSelf();
            }
        } catch (Exception e) {
            running = false;
            updateNotification("VPN başlatılamadı");
            stopForeground(true);
            stopSelf();
        }
    }

    private void stopVpn() {
        running = false;
        if (vpnInterface != null) {
            try { vpnInterface.close(); } catch (Exception ignored) {}
            vpnInterface = null;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "VPN durumu", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Kullanıcının başlattığı VPN servisinin durumu");
            channel.setSound(null, null);
            channel.enableVibration(false);
            channel.setShowBadge(false);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                .createNotificationChannel(channel);
        }
    }

    private Notification makeNotification(String message) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Ağ Yönetimi · VPN")
                .setContentText(message)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setOngoing(true)
                .build();
        }
        return new Notification.Builder(this)
            .setContentTitle("Ağ Yönetimi · VPN")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setOngoing(true)
            .setPriority(Notification.PRIORITY_LOW)
            .setSound(null)
            .setVibrate(new long[] {0L})
            .build();
    }

    private void updateNotification(String message) {
        NotificationManager manager =
            (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, makeNotification(message));
    }

    @Override public void onRevoke() {
        stopVpn();
        stopForeground(true);
        stopSelf();
        super.onRevoke();
    }

    @Override public void onDestroy() {
        stopVpn();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }
}
