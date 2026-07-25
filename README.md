# 🔑 Password Reset Token Architecture (`password-reset-token` Branch)

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

Bu branch, şifresini unutan kullanıcılar için güvenli **Şifre Sıfırlama Token Döngüsü (Secure Password Reset Flow with Hashed Tokens)** mekanizmasını implemente eder.

### Amaç ve Kapsam
Kullanıcıların şifre sıfırlama isteklerinde e-postalarına tek kullanımlık, 1 saat süreli ve veritabanında SHA-256 ile şifrelenerek (hashed) saklanan kurtarma token'ı iletmek ve güvenli biçimde yeni parola belirlemelerini sağlamaktır.

---

## 2. Problem Tanımı

1. **Unutulan Parolalar**: Kullanıcıların sisteme erişimini kaybetmesi.
2. **Düz Metin (Plaintext) Token Saklama Tehlikesi**: Sıfırlama token'ları veritabanında düz metin saklanırsa ve veritabanı sızdırılırsa saldırganlar tüm kullanıcıların şifresini anında değiştirebilir.
3. **Süresiz Token Riski**: Geçerlilik süresi dolmayan token'ların e-posta kutusunda beklemesi ve ileride ele geçirilmesi.

---

## 3. Çözüm Yaklaşımı

* **SHA-256 Hashed Token Storage**:
  * Kullanıcı için `UUID.randomUUID()` ile ham token (`rawToken`) üretilir.
  * `rawToken` e-posta ile kullanıcıya gönderilir.
  * Veritabanına asla `rawToken` yazılmaz; `SHA-256(rawToken)` sonucu olan `hashedToken` kaydedilir.
* **Geçerlilik Süresi (TTL - 1 Saat)**: `expiryDate` varsayılan olarak 3600 saniye (1 saat) olarak belirlenir.
* **Single Active Token Rule**: Yeni şifre sıfırlama isteğinde kullanıcının eski aktif sıfırlama token'ları veritabanından silinir (`deleteByEmployeeId`).
* **Sıfırlama Sonrası İptal**: Yeni şifre başarıyla kaydedildiğinde token DB'den anında silinir.

---

## 4. Kullanılan Teknolojiler

| Teknoloji | Kullanım Amacı |
| :--- | :--- |
| **Java MessageDigest (SHA-256)** | Token'ların veritabanına yazılmadan önce hash'lenmesi. |
| **Java UUID** | 128-bitlik tahmin edilemez rastgele token üretimi. |
| **Spring Boot Starter Mail** | Şifre sıfırlama onay e-postasının gönderilmesi. |
| **BCrypt Password Encoder** | Yeni belirlenen parolanın hash'lenerek kaydedilmesi. |

---

## 5. Proje Mimarisi

```
[ Forgot Password Request ] ──► POST /api/v1/auth/forgot-password
                                         │
                                         ▼
Generate rawToken (UUID) ──► Compute SHA-256(rawToken) ──► Save hashedToken to DB ──► Send rawToken via Email

[ Reset Password Request ]  ──► POST /api/v1/auth/reset-password?token=rawToken
                                         │
                                         ▼
Compute SHA-256(rawToken) ──► Query DB for hashedToken
                                         │
                                         ├──► Found & Valid ──► Update Password with BCrypt ──► Delete Token
                                         └──► Expired / Not Found ──► Throw ResourceNotFoundException (400)
```

---

## 6. Klasör Yapısı

```
src/main/java/com/burakcanaksoy/springsecurity/
├── entity/
│   └── PasswordResetToken.java         # hashedToken ve expiryDate saklayan JPA entity
├── service/
│   └── PasswordResetTokenService.java  # SHA-256 Hashing, Token üretimi ve doğrulama
├── dto/
│   ├── request/ForgotPasswordRequest.java
│   └── request/ResetPasswordRequest.java # Yeni şifre ve @Password validasyonu
└── repository/
    └── PasswordResetTokenRepository.java
```

---

## 7. Kodun Genel Akışı

1. Kullanıcı `/forgot-password` isteği atar (email).
2. `PasswordResetTokenService` rastgele `rawToken` üretir, SHA-256 ile hash'ler, DB'ye `hashedToken` yazar ve kullanıcıya `rawToken` içeren mail atar.
3. Kullanıcı gelen maildeki link ile `/reset-password?token=rawToken` adresine yeni şifresini gönderir.
4. Servis istekle gelen `rawToken`'ı tekrar SHA-256 ile hash'ler ve DB'deki `hashedToken` ile karşılaştırır.
5. Doğrulanırsa şifre BCrypt ile güncellenir ve token silinir.

---

## 8. Önemli Sınıflar

* **`PasswordResetToken`**: `hashedToken`, `employee` ve `expiryDate` tutan entity.
* **`PasswordResetTokenService`**: Kriptografik SHA-256 hash'leme işlemlerini ve token yaşam döngüsünü yöneten servis.

---

## 9. Önemli Teknik Kavramlar

* **One-Way Hashing (SHA-256)**: Geri döndürülemez kriptografik özütleme fonksiyonu. Veritabanı yöneticisi dahi sızan token'ları kullanarak şifre sıfırlayamaz.
* **Token Expiration**: Token'ın kötüye kullanılmasını önlemek için belirli bir zaman dili (1 saat) sonunda hükümsüz kılınması.

---

## 10. Kod Örnekleri

### SHA-256 Token Hash'leme Mantığı (`PasswordResetTokenService.java`)
```java
private String hashToken(String token) {
    try {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException("SHA-256 algorithm not found", e);
    }
}
```

---

## 11. API Açıklamaları

| Method | Endpoint | Body / Param | Açıklama |
| :--- | :--- | :--- | :--- |
| **POST** | `/api/v1/auth/forgot-password` | `ForgotPasswordRequest` | E-postaya şifre sıfırlama linki gönderir. |
| **POST** | `/api/v1/auth/reset-password` | `?token=rawToken`, `ResetPasswordRequest` | Sıfırlama token'ını doğrular ve yeni şifreyi günceller. |

---

## 12. Kurulum

1. Mail sunucusu ayarlarını tamamlayın.
2. Projeyi çalıştırıp `/forgot-password` ve `/reset-password` akışını test edin.

---

## 13. Konfigürasyon

* Token geçerlilik süresi 1 saattir (`Instant.now().plusSeconds(3600)`).

---

## 14. Güvenlik

* **Database Leak Immunity**: Veritabanı çalınsa dahi `hashedToken` değerlerinden `rawToken` elde edilemez.
* **Password Validation**: Yeni şifre belirlenirken de `@Password` anotasyonu kuralları zorunlu olarak uygulanır.

---

## 15. Veri Akışı

```
Forgot Request -> Generate UUID -> SHA-256 Hash -> Save DB -> Send Raw Email -> User Reset Request -> Hash Input -> Match DB Hash -> Update Password
```

---

## 16. Hata Yönetimi

* Geçersiz veya süresi dolmuş token'larda `ResourceNotFoundException("Invalid password reset token")` fırlatılır.

---

## 17. Performans

* SHA-256 hesabı microsecond seviyesinde olduğu için CPU'ya ekstra yük getirmez.

---

## 18. Geliştirici Notları

* Şifre başarıyla güncellendikten sonra kullanıcının mevcut tüm aktif Refresh Token'ları silinerek diğer cihazlardaki oturumları kapatılmalıdır.

---

## 19. Gelecekte Yapılabilecek Geliştirmeler

- [ ] Şifre değiştirildiğinde tüm aktif oturumların (Refresh Token) otomatik sonlandırılması.
