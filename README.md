# Ağ Yönetimi — Android WireGuard VPN

Uygulama Android'in kullanıcı onaylı VPN arayüzünü ve WireGuard tünel altyapısını kullanır. Kullanıcı, güvendiği sağlayıcıdan aldığı `.conf` dosyasını uygulamaya içe aktarabilir. Sağlayıcı yapılandırmasında `AllowedIPs = 0.0.0.0/0, ::/0` varsa Full Tunnel rotaları ve sağlayıcının DNS ayarları desteklenir.

## Güvenlik ve kullanım

- Özel anahtarlar uygulama günlüklerine yazılmaz ve yapılandırma dosyası uygulama tarafından kalıcı olarak kaydedilmez. Uygulama yeniden başlatılınca yapılandırmayı tekrar içe aktarın.
- Yalnızca güvendiğiniz VPN sağlayıcısından aldığınız WireGuard yapılandırmasını kullanın. Özel anahtarınızı kimseyle paylaşmayın.
- Full Tunnel, internet trafiğini VPN'e yönlendirir. VPN bağlantısı başarısız olursa veya sunucu erişilemezse internet erişimi etkilenebilir.
- VPN'i başlatmak için Android'in sistem VPN iznini onaylamanız gerekir.

## Derleme

GitHub Actions, `main` dalına gönderim yapıldığında veya Actions sekmesinden manuel başlatıldığında Android APK/APKS üretmeyi dener. Derleme başarılı olduğunda dosyalar Actions artifact bölümünde bulunur.
