# 🔐 2FA Email OTP Architecture (`2fa-email-otp` Branch)

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

Bu branch, `master` üzerindeki temel JWT mimarisine **İki Aşamalı Doğrulama (2FA - Two-Factor Authentication via Email OTP)** mekanizmasını ekler.

### Amaç ve Kapsam
Sistemdeki kullanıcı oturum açma (login) sürecini tek faktörlü (sadece şifre) olmaktan çıkararak 6 haneli, süreli ve tek kullanımlık (OTP - One-Time Password) e-posta kodları ile 2. bir doğrulama katmanıyla koruma altına almaktır.

### Gerçek Hayat Senaryoları
Finans uygulamaları, kritik personel yönetim sistemleri ve KVKK/GDPR uyumluluğu gerektiren kurumsal sistemlerde kullanıcı hesaplarının sadece parola sızıntısı ile ele geçirilmesini engellemek.

---

## 2. Problem Tanımı

Sadece kullanıcı adı ve parolaya dayalı kimlik doğrulama modelleri (Single-Factor Authentication) aşağıdaki tehlikelere açık haldedir:
1. **Credential Stuffing & Password Reuse**: Kullanıcıların farklı sitelerde aynı şifreyi kullanması sonucu parolanın ele geçirilmesi.
2. **Phishing & Keylogger**: Kullanıcının şifresini zararlı yazılımlarla sızdırması.
3. **Brute-Force Attacks**: Kolay tahmin edilebilir şifrelerin otomatik araçlarla kırılması.

---

## 3. Çözüm Yaklaşımı

Bu branch'te **Email OTP tabanlı 2FA Mimarisi** uygulanmıştır:
* **Kriptografik Rastgele Kod Üretimi**: Java `SecureRandom` kullanılarak 6 haneli, tahmin edilemez sayısal OTP kodları üretilir.
* **Geçici Ömür (TTL - Time To Live)**: Üretilen OTP kodları veritabanında saklanır ve varsayılan olarak **5 dakika** geçerlilik süresine (`expiryDate`) sahiptir.
* **Tek Kullanımlık Mantık (`used` flag)**: Başarıyla doğrulanan OTP kodu anında `used=true` olarak işaretlenir veya veritabanından temizlenir. Tekrar kullanılamaz.
* **Veritabanı Temizliği**: Kullanıcı yeni bir OTP talep ettiğinde eski henüz kullanılmamış token'lar silinir.

---

## 4. Kullanılan Teknolojiler

| Teknoloji | Kullanım Amacı |
| :--- | :--- |
| **Java 17 & Spring Boot 3.x/4.x** | Core mimarı framework ve bağımlılık yönetimi. |
| **Spring Boot Starter Mail** | JavaMailSender ve SMTP protokolü üzerinden e-posta gönderimi. |
| **SecureRandom** | Kriptografik olarak güvenli 6 haneli OTP kodu üretimi. |
| **Spring Data JPA & MySQL** | `otp_token` tablosunun ORM ile yönetilmesi ve ilişkisel saklanması. |
| **Spring Security & JWT** | OTP doğrulaması sonrasında Access Token / Refresh Token dağıtımı. |

---

## 5. Proje Mimarisi

```
[ User Login Attempt ] 
       │
       ▼
[ AuthService.login() ] ──► (Verify Username/Password)
       │
       ▼
[ OtpTokenService ] ──► (Generate 6-digit OTP & Save DB)
       │
       ▼
[ EmailService ] ──► (Send OTP via SMTP to User Email)
       │
[ User Inputs OTP Code ]
       │
       ▼
[ POST /api/v1/auth/verify-otp ] ──► (Verify Expiry & Used Status)
       │
       ▼
[ Issue JWT Access & Refresh Token ]
```

---

## 6. Klasör Yapısı

```
src/main/java/com/burakcanaksoy/springsecurity/
├── entity/
│   ├── OtpToken.java                   # 6 Haneli OTP Kod Entity'si (expiryDate, used)
│   ├── Employee.java
│   └── RefreshToken.java
├── repository/
│   └── OtpTokenRepository.java         # OTP Sorgulama (findValidOtp)
├── service/
│   ├── OtpTokenService.java            # OTP Üretim, Doğrulama, Expiry Kontrolü
│   ├── EmailService.java               # SMTP ile E-posta Gönderimi
│   └── AuthService.java                # 2FA Login Orkestrasyonu
└── controller/
    └── AuthController.java             # /verify-otp Uç Noktası
```

---

## 7. Kodun Genel Akışı

1. Kullanıcı `/login` isteği atar (kullanıcı adı & şifre).
2. Sistem şifreyi doğrular ancak henüz tam JWT üretmez; `OtpTokenService.createOtpToken()` ile 6 haneli OTP oluşturur.
3. `EmailService` OTP kodunu e-posta olarak gönderir.
4. İstemci e-postadaki 6 haneli kodu `/verify-otp?code=123456` uç noktasına gönderir.
5. `OtpTokenService.verifyOtp()` kodun süresini ve geçerliliğini kontrol eder. Kod geçerliyse `used=true` yapılır ve istemciye `LoginResponse` (JWT Token seti) teslim edilir.

---

## 8. Önemli Sınıflar

* **`OtpToken`**: OTP verisini (`otpCode`, `expiryDate`, `used`, `employee`) tutan JPA entity sınıfı.
* **`OtpTokenService`**: `SecureRandom` ile kod üreten ve doğrulayan servis.
* **`EmailService`**: `JavaMailSender` nesnesini kullanarak kullanıcının e-posta adresine OTP mesajını ileten bileşen.

---

## 9. Önemli Teknik Kavramlar

* **OTP (One-Time Password)**: Tek bir oturum veya işlem için geçerli olan, kullanıldıktan sonra hükmünü yitiren şifre.
* **SecureRandom**: `java.util.Random` yerine kriptografik olarak tahmin edilemez (PRNG) rastgele sayılar üreten Java sınıfı.
* **SMTP (Simple Mail Transfer Protocol)**: E-posta gönderimi için kurulan standart ağ protokolü.

---

## 10. Kod Örnekleri

### OTP Üretim Mantığı (`OtpTokenService.java`)
```java
@Transactional
public OtpToken createOtpToken(Employee employee) {
    otpTokenRepository.deleteByEmployeeId(employee.getId());

    String otpCode = String.format("%06d", RANDOM.nextInt(999999));

    OtpToken otpToken = OtpToken.builder()
            .employee(employee)
            .otpCode(otpCode)
            .expiryDate(Instant.now().plus(5, ChronoUnit.MINUTES))
            .used(false)
            .build();

    return otpTokenRepository.save(otpToken);
}
```

---

## 11. API Açıklamaları

| Method | Endpoint | Parametre / Body | Açıklama |
| :--- | :--- | :--- | :--- |
| **POST** | `/api/v1/auth/login` | `EmployeeLoginRequest` | Giriş bilgileri doğrulanır, e-postaya OTP gönderilir. |
| **POST** | `/api/v1/auth/verify-otp` | `?code=123456` | OTP kodunu doğrular ve JWT token setini döner. |

---

## 12. Kurulum

1. `application.properties` içerisine SMTP sunucu bilgilerinizi ekleyin:
   ```properties
   spring.mail.host=smtp.gmail.com
   spring.mail.port=587
   spring.mail.username=your-email@gmail.com
   spring.mail.password=your-app-password
   spring.mail.properties.mail.smtp.auth=true
   spring.mail.properties.mail.smtp.starttls.enable=true
   ```
2. Projeyi derleyip çalıştırın:
   ```bash
   mvn spring-boot:run
   ```

---

## 13. Konfigürasyon

OTP süresi `OtpTokenService` içerisindeki `OTP_EXPIRY_MINUTES = 5` sabiti ile ayarlanabilir.

---

## 14. Güvenlik

* OTP kodu asla loglara yazılmamalıdır.
* Her yeni doğrulama isteğinde eski OTP kodları geçersiz kılınır.
* Yanlış kod denemelerinde kaba kuvvet (brute-force) engellemesi için bu mekanizmaya Rate-Limiting eklenmesi önerilir.

---

## 15. Veri Akışı

```
User -> Login -> DB Check -> SecureRandom OTP -> Mail Server -> User Email -> Verify OTP -> JWT Issued
```

---

## 16. Hata Yönetimi

* OTP süresi dolmuşsa veya yanlışsa `ResourceNotFoundException("Invalid or expired OTP code.")` fırlatılır.

---

## 17. Performans

* OTP kayıtlarının veritabanında birikmesini önlemek için periyodik bir Scheduled Task ile süresi dolmuş kayıtların temizlenmesi önerilir.

---

## 18. Geliştirici Notları

* E-posta gönderiminin senkron yapılması HTTP isteğini geciktirebilir. Üretim ortamlarında e-posta gönderimi `@Async` olarak yapılandırılmalıdır.

---

## 19. Gelecekte Yapılabilecek Geliştirmeler

- [ ] Asenkron E-posta gönderimi (`@EnableAsync`).
- [ ] OTP deneme sayısı sınırlaması (Max 3 deneme hakkı).
- [ ] HTML e-posta şablonları (Thymeleaf email templates).
