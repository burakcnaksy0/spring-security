# 📱 2FA Google Authenticator (TOTP) Architecture (`2fa-google-authenticator` Branch)

---

## 📑 İçindekiler
1. [Proje Tanıtımı](#1-proje-tanıtımı)
2. [Problem Tanımı](#2-problem-tanımı)
3. [Çözüm Yaklaşımı](#3-çözüm-yaklaşımı)
4. [Kullanılan Teknolojiler](#4-kullanılan-teknolojiler)
5. [Proje Mimarisi](#5-proje-mimarisi)
6. [Klasör Yapısı](#6-klasör-yapısı)
7. [Kodun Genel Akışı](#7-kodun-genel-akışı)
8. [Önemli Sınıflar](#8-önemli-sınıflar)
9. [Önemli Teknik Kavramlar](#9-önemli-teknik-kavramlar)
10. [Kod Örnekleri](#10-kod-örnekleri)
11. [API Açıklamaları](#11-api-açıklamaları)
12. [Kurulum](#12-kurulum)
13. [Konfigürasyon](#13-konfigürasyon)
14. [Güvenlik](#14-güvenlik)
15. [Veri Akışı](#15-veri-akışı)
16. [Hata Yönetimi](#16-hata-yönetimi)
17. [Performans](#17-performans)
18. [Geliştirici Notları](#18-geliştirici-notları)
19. [Gelecekte Yapılabilecek Geliştirmeler](#19-gelecekte-yapılabilecek-geliştirmeler)

---

## 1. Proje Tanıtımı

Bu branch, uygulamaya **Zaman Tabanlı Tek Kullanımlık Şifre (TOTP - Time-Based One-Time Password)** mantığıyla çalışan **Google Authenticator / Authy / Microsoft Authenticator** entegrasyonu ekler.

### Amaç ve Kapsam
E-posta ile 2FA iletimindeki ağ gecikmelerini ve SMTP sunucu bağımlılıklarını ortadan kaldırarak istemci tarafında tamamen internet bağlantısı bile gerektirmeyen (offline) 30 saniyelik dinamik TOTP kodları ile 2 aşamalı doğrulama sağlamaktır.

### Gerçek Hayat Senaryoları
Yüksek güvenlik gerektiren bankacılık uygulamaları, kripto borsa sistemleri ve AWS/Google Cloud gibi bulut yönetim konsolu girişleri.

---

## 2. Problem Tanımı

Email tabanlı 2FA sistemlerinin bazı kısıtlamaları mevcuttur:
1. **E-posta İletim Gecikmeleri**: SMTP sunucularındaki kuyruklar veya e-postanın spam klasörüne düşmesi.
2. **E-posta Hesabının Ele Geçirilmesi**: Kullanıcının e-posta hesabı çalındığında 2FA koruması da çöker.
3. **İnternet Bağımlılığı**: Kullanıcı e-postalarını anlık kontrol edemediği durumlarda (örneğin uçak modu veya çekmeyen alanlar) sisteme giremez.

---

## 3. Çözüm Yaklaşımı

RFC 6238 standardına uygun **TOTP (Time-Based One-Time Password)** çözümü uygulanmıştır:
* **Secret Key Generation**: Kullanıcı için Base32 formatında benzersiz bir gizli anahtar (`secretKey`) üretilir ve veritabanında saklanır.
* **QR Code Rendering**: `dev.samstevens.totp` kütüphanesi ve ZXing motoru kullanılarak `otpauth://totp/SpringSecurityApp:username?secret=...` URI formatında Base64 PNG QR kod görseli oluşturulur.
* **TOTP Verification**: Sunucu ve mobil uygulama aynı gizli anahtarı ve geçerli Unix Timestamp bilgisini kullanarak HMAC-SHA1 algoritması ile 30 saniyede bir aynı 6 haneli kodu bağımsız olarak hesaplar ve doğrular.

---

## 4. Kullanılan Teknolojiler

| Teknoloji | Kullanım Amacı |
| :--- | :--- |
| **`dev.samstevens.totp`** (2.3.0) | Java TOTP secret generator, QR code generator ve time-provider doğrulama kütüphanesi. |
| **ZXing (Zebra Crossing)** | QR Kod PNG resim matrislerinin Java içinde üretilmesi. |
| **Java 17 / Spring Boot 3.x** | Core backend mimarisi ve Controller/Service katmanı. |
| **Spring Data JPA & MySQL** | Kullanıcının MFA durumunu (`mfaEnabled`) ve `secretKey` bilgisini saklama. |

---

## 5. Proje Mimarisi

```
[ Setup Phase ]
User ──► POST /api/v1/mfa/setup ──► TotpService (Generate Secret & QR Code) ──► Scan via Google Auth App

[ Enable Phase ]
User ──► POST /api/v1/mfa/enable?code=123456 ──► TotpService.verifyCode() ──► mfaEnabled = true in DB

[ Login Phase ]
User ──► POST /api/v1/auth/login ──► Check mfaEnabled == true ──► Prompt for TOTP Code ──► Validate ──► Issue JWT
```

---

## 6. Klasör Yapısı

```
src/main/java/com/burakcanaksoy/springsecurity/
├── controller/
│   └── MfaController.java             # /mfa/setup ve /mfa/enable Uç Noktaları
├── service/
│   └── TotpService.java               # Secret generation, QR Code Base64 generation, Code Verification
├── dto/
│   ├── response/TotpSetupResponse.java# Base64 QR Image & Raw Secret Key DTO'su
│   └── response/MfaEnableResponse.java# 2FA Aktifleşme durum yanıtı
└── entity/
    └── Employee.java                  # secretKey ve mfaEnabled alanları
```

---

## 7. Kodun Genel Akışı

1. **Kurulum (`/setup`)**: Kullanıcı oturum açtıktan sonra `/api/v1/mfa/setup` çağrısı yapar. Sistem gizli bir key üretir ve Base64 PNG QR kod yanıtı döner.
2. **Kullanıcı İşlemi**: Kullanıcı mobil Google Authenticator uygulamasıyla ekranındaki QR kodu taratır.
3. **Aktifleştirme (`/enable`)**: Kullanıcı uygulamada oluşan 6 haneli ilk kodu `/api/v1/mfa/enable?code=xxxxxx` uç noktasına gönderir. Kod doğrulanırsa kullanıcının DB'deki `mfaEnabled` alanı `true` yapılır.
4. **Sonraki Girişler**: Giriş sırasında şifre doğrulandıktan sonra TOTP kodu istenir.

---

## 8. Önemli Sınıflar

* **`TotpService`**: `DefaultSecretGenerator`, `ZxingPngQrGenerator`, `SystemTimeProvider` ve `DefaultCodeVerifier` bileşenlerini sarmalayarak TOTP mantığını yürüten servis.
* **`MfaController`**: 2FA kurulum ve aktifleştirme isteklerini karşılayan REST denetleyicisi.

---

## 9. Önemli Teknik Kavramlar

* **TOTP (RFC 6238)**: Gizli anahtar ve o anki Unix zaman dilimi (30 saniyelik pencereler) kullanılarak hesaplanan zaman tabanlı şifre.
* **Base32 Encoding**: TOTP secret anahtarlarının e-posta ve ekranlarda okunabilir kalmasını sağlayan 32 karakterlik alfabe.
* **HMAC-SHA1**: Hash-based Message Authentication Code; TOTP üretiminde kullanılan temel kriptografik özüt fonksiyonu.

---

## 10. Kod Örnekleri

### QR Kod Üretim Kod Parçası (`TotpService.java`)
```java
public String generateQrCodeImage(String secret, String username) {
    QrData data = new QrData.Builder()
            .label(username)
            .secret(secret)
            .issuer("SpringSecurityApp")
            .algorithm(HashingAlgorithm.SHA1)
            .digits(6)
            .period(30)
            .build();
    try {
        byte[] imageBytes = qrGenerator.generate(data);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
    } catch (QrGenerationException e) {
        throw new RuntimeException("QR kod üretilemedi", e);
    }
}
```

---

## 11. API Açıklamaları

| Method | Endpoint | Erişim | Açıklama |
| :--- | :--- | :--- | :--- |
| **POST** | `/api/v1/mfa/setup` | Authenticated | TOTP gizli anahtarı ve Base64 QR resmini döner. |
| **POST** | `/api/v1/mfa/enable` | Authenticated | 6 haneli TOTP kodunu doğrular ve hesabı 2FA aktif hale getirir. |

---

## 12. Kurulum

1. Maven bağımlılığını kontrol edin (`pom.xml`):
   ```xml
   <dependency>
       <groupId>dev.samstevens.totp</groupId>
       <artifactId>totp</artifactId>
       <version>2.3.0</version>
   </dependency>
   ```
2. Projeyi çalıştırın ve Swagger üzerinden `/api/v1/mfa/setup` endpoint'ini test edin.

---

## 13. Konfigürasyon

* `issuer`: "SpringSecurityApp" (Google Authenticator uygulamasında görünen uygulama adı).
* `period`: 30 saniye.

---

## 14. Güvenlik

* **Secret Key Encryption**: Veritabanındaki `secretKey` alanı üretim ortamlarında simetrik şifreleme (AES-256) ile şifrelenerek saklanmalıdır.
* **Clock Drift**: Sunucu saati ile mobil cihaz saati arasındaki farkı tolere etmek için `TimeProvider` drift ayarları yapılandırılabilir.

---

## 15. Veri Akışı

```
Client -> /mfa/setup -> Generate Secret -> Render Base64 QR -> Mobile App Scan -> /mfa/enable (Code) -> Verify HMAC-SHA1 -> Enable 2FA in DB
```

---

## 16. Hata Yönetimi

* Geçersiz veya süresi dolmuş TOTP kodlarında `400 Bad Request` veya custom exception ile hata döndürülür.

---

## 17. Performans

* TOTP doğrulaması tamamen hafıza (in-memory) matematiksel hesaplamasıdır. Veritabanına ekstra I/O yükü getirmez, son derece hızlıdır.

---

## 18. Geliştirici Notları

* Cihazını kaybeden kullanıcılar için sisteme **Recovery Codes (Kurtarma Kodları)** eklenmesi önerilir.

---

## 19. Gelecekte Yapılabilecek Geliştirmeler

- [ ] Tek kullanımlık 8 haneli Recovery (Kurtarma) kodları üretimi.
- [ ] SMS / Push notification fallback desteği.
