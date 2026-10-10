package de.furkanmeyra.networkmanager;

import android.content.Context;

import com.wireguard.android.backend.Backend;
import com.wireguard.android.backend.GoBackend;
import com.wireguard.android.backend.Tunnel;
import com.wireguard.config.Config;
import com.wireguard.config.InetNetwork;
import com.wireguard.config.Peer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * In-memory WireGuard controller. Configuration is never logged or persisted.
 * To protect ordinary internet access, this app only accepts split-tunnel
 * configurations: no IPv4/IPv6 default route and no VPN-wide DNS override.
 */
public final class WireGuardController {
    private WireGuardController() {}

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static volatile Config config;
    private static volatile boolean connected;
    private static volatile String lastStatus = "WireGuard yapılandırması eklenmedi";
    private static Backend backend;

    private static final Tunnel TUNNEL = new Tunnel() {
        @Override public String getName() { return "AgYonetimi"; }
        @Override public void onStateChange(State state) {
            connected = state == State.UP;
            lastStatus = connected ? "WireGuard bağlantısı açık" : "WireGuard bağlantısı kapalı";
        }
    };

    public static void setConfig(Config value) {
        config = value;
        lastStatus = "WireGuard yapılandırması hazır (yalnızca bölünmüş tünel)";
    }

    public static boolean hasConfig() { return config != null; }
    public static boolean isConnected() { return connected; }
    public static String getStatus() { return lastStatus; }

    public static void validateSafeSplitTunnel(Config candidate) {
        if (candidate == null || candidate.getPeers().isEmpty()) {
            throw new IllegalArgumentException("Dosyada en az bir WireGuard [Peer] bölümü bulunmalı.");
        }
        if (candidate.getInterface().getAddresses().isEmpty()) {
            throw new IllegalArgumentException("[Interface] bölümünde Address gerekli.");
        }
        if (!candidate.getInterface().getDnsServers().isEmpty()) {
            throw new IllegalArgumentException("Güvenlik için DNS satırları kabul edilmiyor; normal internet DNS ayarları korunmalı.");
        }

        boolean hasRoute = false;
        for (Peer peer : candidate.getPeers()) {
            if (!peer.getEndpoint().isPresent()) {
                throw new IllegalArgumentException("Her eş için Endpoint tanımlanmalı.");
            }
            for (InetNetwork network : peer.getAllowedIps()) {
                // A zero-length prefix is a default route and can capture all traffic.
                if (network.getMask() <= 1) {
                    throw new IllegalArgumentException("Varsayılan veya çok geniş /0-/1 rotaları güvenlik için engellendi. Yalnızca sınırlı bölünmüş tünel yapılandırması kullan.");
                }
                hasRoute = true;
            }
        }
        if (!hasRoute) {
            throw new IllegalArgumentException("Peer bölümünde AllowedIPs rotası bulunamadı.");
        }
    }

    public static void connect(Context context, Consumer<String> callback) {
        EXECUTOR.execute(() -> {
            Config current = config;
            if (current == null) {
                callback.accept("Önce bir WireGuard .conf dosyası içe aktar.");
                return;
            }
            try {
                validateSafeSplitTunnel(current);
                if (backend == null) backend = new GoBackend(context.getApplicationContext());
                backend.setState(TUNNEL, Tunnel.State.UP, current);
                connected = backend.getState(TUNNEL) == Tunnel.State.UP;
                lastStatus = connected ? "WireGuard bağlı (bölünmüş tünel)" : "WireGuard bağlantısı kurulamadı";
                callback.accept(lastStatus);
            } catch (Exception e) {
                // Tear down any partially-created tunnel; no default route was accepted.
                try {
                    if (backend != null) backend.setState(TUNNEL, Tunnel.State.DOWN, null);
                } catch (Exception ignored) {}
                connected = false;
                lastStatus = "Bağlantı kurulamadı; normal internet rotası korunuyor.";
                callback.accept(lastStatus + " Ayrıntı: " + safeMessage(e));
            }
        });
    }

    public static void disconnect(Consumer<String> callback) {
        EXECUTOR.execute(() -> {
            try {
                if (backend != null) backend.setState(TUNNEL, Tunnel.State.DOWN, null);
                connected = false;
                lastStatus = "WireGuard bağlantısı kapalı";
                callback.accept(lastStatus);
            } catch (Exception e) {
                callback.accept("Bağlantı kapatılamadı: " + safeMessage(e));
            }
        });
    }

    private static String safeMessage(Exception e) {
        String message = e.getMessage();
        if (message == null || message.trim().isEmpty()) return e.getClass().getSimpleName();
        // Avoid echoing configuration text or keys into UI/logs.
        return message.length() > 160 ? message.substring(0, 160) : message;
    }
}
