# 📧 Email Account Verification Architecture (`email-verification` Branch)

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

Bu branch, yeni kaydolan kullanıcı hesaplarının gerçeğe uygunluğunu doğrulamak amacıyla **E-posta Hesabı Aktifleştirme (Email Verification Token Lifecycle)** mekanizmasını sisteme dahil etmektedir.

### Amaç ve Kapsam
Sistemde rastgele veya sahte (fake/disposable) e-posta adresleriyle hesapsız kullanıcı kaydı yapılmasını engellemek, e-posta mülkiyeti doğrulanana kadar hesabı pasif tutmak (`enabled = false`) ve doğrulama linki üzerinden aktif etmektir.

---

## 2. Problem Tanımı

1. **Sahte Hesap ve Spam Kayıtlar**: Doğrulama adımı olmaksızın herkes istediği e-posta adresiyle sistemde kayıt açabilir (Identity Theft).
2. **Kötüye Kullanım (Abuse)**: Başkasının e-posta adresiyle hesap açılarak sistem üzerinden bildirim veya e-posta gönderimi yapılması.

---

## 3. Çözüm Yaklaşımı

* **Account Status (`enabled=false`)**: Kullanıcı `/register` olduğunda DB'de `enabled` varsayılan olarak `false` kaydedilir.
* **UUID Verification Token**: `UUID.randomUUID()` ile benzersiz, tahmin edilemez 128-bitlik onay token'ı üretilir ve `verification_token` tablosunda saklanır (TTL: 24 Saat).
* **Email Link Dispatch**: `EmailService` üzerinden kullanıcı e-posta adresine `http://localhost:8080/api/v1/auth/verify?token=...` onay linki gönderilir.
* **Login Guard (`EmailNotVerifiedException`)**: Giriş sırasında `enabled == false` ise `EmailNotVerifiedException` fırlatılır ve oturum açma engellenir.

---

## 4. Kullanılan Teknolojiler

| Teknoloji | Kullanım Amacı |
| :--- | :--- |
| **Java UUID** | Kriptografik olarak güvenli 36 karakterlik doğrulama token'ı üretimi. |
| **Spring Boot Starter Mail** | Onay e-postası iletimi. |
| **Spring Data JPA & MySQL** | `verification_token` tablosu ve `@OneToOne` ilişki yönetimi. |

---

## 5. Proje Mimarisi

```
[ User Register ] ──► Employee (enabled=false) ──► Generate VerificationToken (UUID) ──► Send Email Link

[ User Clicks Email Link ] ──► GET /api/v1/auth/verify?token=UUID
                                       │
                                       ▼
VerificationTokenService.verifyToken() ──► Check Expiry ──► Employee.setEnabled(true)

[ User Login Attempt ] ──► Check Employee.isEnabled() 
                               ├──► TRUE  ──► Issue JWT Tokens
                               └──► FALSE ──► Throw EmailNotVerifiedException (403/400)
```

---

## 6. Klasör Yapısı

```
src/main/java/com/burakcanaksoy/springsecurity/
├── entity/
│   └── VerificationToken.java         # UUID onay token'ı entity'si
├── service/
│   ├── VerificationTokenService.java  # Token oluşturma ve onaylama servisi
│   └── EmailService.java              # Onay maili gönderici servisi
├── exception/
│   └── EmailNotVerifiedException.java # Hesabı onaylanmamış kullanıcı istisnası
└── repository/
    └── VerificationTokenRepository.java
```

---

## 7. Kodun Genel Akışı

1. Kullanıcı kaydolur. `AuthService.register()` kullanıcının `enabled` durumunu `false` yapar.
2. `VerificationTokenService` 24 saatlik UUID token'ı oluşturur ve veritabanına yazar.
3. Kullanıcıya doğrulama linki e-posta atılır.
4. Kullanıcı onay linkine tıklar. `GET /api/v1/auth/verify` çağrılır.
5. Token geçerliyse kullanıcının `enabled` alanı `true` yapılır ve doğrulama token'ı DB'den silinir.

---

## 8. Önemli Sınıflar

* **`VerificationToken`**: Token string'i, `employee` ilişkisi ve `expiryDate` barındıran entity.
* **`VerificationTokenService`**: Token doğrulama ve kullanıcı aktivasyon mantığı.
* **`EmailNotVerifiedException`**: Onaylanmamış hesap giriş denemelerinde fırlatılan özel istisna.

---

## 9. Önemli Teknik Kavramlar

* **Account Activation**: Hesabın mülkiyet kanıtlanana kadar kilitli tutulması prensibi.
* **UUID (Universally Unique Identifier)**: 128-bitlik benzersiz kimlik üreteci.

---

## 10. Kod Örnekleri

### Doğrulama Uç Noktası (`AuthController.java`)
```java
@GetMapping("/verify")
public ResponseEntity<String> verifyEmail(@RequestParam String token) {
    return ResponseEntity.ok(authService.verifyEmail(token));
}
```

---

## 11. API Açıklamaları

| Method | Endpoint | Parametre | Açıklama |
| :--- | :--- | :--- | :--- |
| **GET** | `/api/v1/auth/verify` | `?token=UUID` | Kullanıcının e-posta hesabını doğrular ve aktif eder. |

---

## 12. Kurulum

1. Mail sunucu ayarlarını `application.properties` dosyasına ekleyin.
2. Projeyi çalıştırıp `/register` ile kullanıcı oluşturun ve e-postanıza gelen link ile doğrulayın.

---

## 13. Konfigürasyon

* Token ömrü varsayılan 24 saattir (`Instant.now().plus(24, ChronoUnit.HOURS)`).

---

## 14. Güvenlik

* Doğrulanmamış hesaplar sisteme erişemez.
* Süresi dolan token'lar otomatik reddedilir.

---

## 15. Veri Akışı

```
Register -> Inactive User -> Save UUID -> Email Dispatch -> User Click Link -> Mark Active -> Enabled
```

---

## 16. Hata Yönetimi

* Doğrulanmamış hesapla login denemesinde `EmailNotVerifiedException` fırlatılır ve `GlobalExceptionHandler` tarafından yakalanır.

---

## 17. Performans

* Token doğrulandıktan sonra veritabanından silindiği için `verification_token` tablosu şişmez.

---

## 18. Geliştirici Notları

* E-posta ulaşmama durumları için `/resend-verification-token` endpoint'i eklenmesi faydalı olacaktır.

---

## 19. Gelecekte Yapılabilecek Geliştirmeler

- [ ] Onay mailini yeniden gönderme (`/resend-verification`) desteği.
