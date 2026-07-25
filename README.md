# 🌐 OAuth2 Google Social Login Architecture (`oath2-google` Branch)

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

Bu branch, sisteme **Google OAuth2 / OpenID Connect (OIDC) Sosyal Giriş (Social Login)** altyapısını entegre eder.

### Amaç ve Kapsam
Kullanıcıların yeni şifre oluşturmadan tek tıkla Google hesaplarıyla sisteme giriş yapabilmesini (SSO), var olan hesapların Google hesaplarıyla bağlanmasını (Account Linking) ve OAuth2 doğrulaması sonrası sistem içi JWT (Access & Refresh) token'larının otomatik üretilmesini sağlamaktır.

---

## 2. Problem Tanımı

1. **Parola Yorgunluğu (Password Fatigue)**: Kullanıcıların her sistem için ayrı şifre hatırlamak zorunda kalması.
2. **Kullanıcı Kayıt Sürtünmesi (Registration Friction)**: Form doldurma ve e-posta doğrulama adımlarının kullanıcı kaybına yol açması.
3. **Güvenli Kimlik Sağlayıcı İhtiyacı**: Google gibi dünya standartlarında kimlik doğrulama sunan sistemlerin güvenliğinden faydalanma isteği.

---

## 3. Çözüm Yaklaşımı

* **OAuth2 / OIDC Protocol**: Spring Security `spring-boot-starter-oauth2-client` modülü kullanılarak Google Authorization Server ile entegrasyon kurulmuştur.
* **Custom OAuth2 Success Handler (`OAuth2LoginSuccessHandler`)**: Google'dan dönen ID Token & Principal içindeki `email`, `sub` (provider ID), `name` ve `email_verified` bilgileri okunur.
* **Auto-Provisioning & Link Logic**:
  * Eğer e-posta veritabanında varsa ve önceden yerel kaydolmuşsa Google `providerId` bilgisi mevcut hesaba bağlanır (`linkGoogleIfNeeded`).
  * Eğer e-posta veritabanında yoksa yeni bir `Employee` otomatik oluşturulur (`AuthProvider.GOOGLE`, `enabled = true`).
* **JWT Bridging**: OAuth2 girişi sonrasında harici kullanıcıya yerel uygulamanın JWT (Access & Refresh Token) verileri teslim edilir.

---

## 4. Kullanılan Teknolojiler

| Teknoloji | Kullanım Amacı |
| :--- | :--- |
| **Spring Security OAuth2 Client** | Google OAuth2/OIDC protokol akışının (Authorization Code Grant) yönetimi. |
| **Google Cloud Console Credentials** | OAuth2 `client-id` ve `client-secret` yapılandırması. |
| **AuthProvider Enum** | Kullanıcının giriş kaynağının (`LOCAL`, `GOOGLE`) veritabanında takibi. |

---

## 5. Proje Mimarisi

```
[ User Clicks Login with Google ] 
       │
       ▼
Redirect to accounts.google.com ──► User Consents ──► Redirect Back with Authorization Code
       │
       ▼
Spring OAuth2Client ──► Exchange Code for Access/ID Token with Google
       │
       ▼
[ OAuth2LoginSuccessHandler ]
       ├── Check Google email_verified == true
       ├── Find / Register Employee in Database (Set AuthProvider.GOOGLE)
       └── Issue Local Access & Refresh JWT Tokens
```

---

## 6. Klasör Yapısı

```
src/main/java/com/burakcanaksoy/springsecurity/
├── exception/
│   └── OAuth2LoginSuccessHandler.java  # Custom OAuth2 Başarılı Giriş İşleyicisi
├── entity/
│   ├── AuthProvider.java               # LOCAL, GOOGLE Enum'ları
│   └── Employee.java                   # provider, providerId alanları
└── config/
    └── SecurityConfig.java             # oauth2Login() filter chain yapılandırması
```

---

## 7. Kodun Genel Akışı

1. Kullanıcı `/oauth2/authorization/google` adresine yönlendirilir.
2. Google hesabını seçip izin verir.
3. Google, uygulamayı `/login/oauth2/code/google` adresine yönlendirir.
4. `OAuth2LoginSuccessHandler` tetiklenir.
5. Kullanıcı veritabanında yoksa otomatik oluşturulur (`registerNewOAuthEmployee`).
6. Uygulama kendi JWT Access ve Refresh Token'larını üreterek istemciye döner.

---

## 8. Önemli Sınıflar

* **`OAuth2LoginSuccessHandler`**: OAuth2 doğrulamasından sonra çalışan, hesabı veritabanıyla eşleştiren ve JWT üreten çekirdek işleyici.
* **`AuthProvider`**: Hesabın nereden açıldığını gösteren enum (`LOCAL`, `GOOGLE`).

---

## 9. Önemli Teknik Kavramlar

* **OAuth2**: İstemcilerin kaynak sunuculara sınırlı erişim sağlamasına izin veren yetkilendirme çerçevesi.
* **OpenID Connect (OIDC)**: OAuth 2.0 üzerine kurulmuş kimlik doğrulama (Authentication) katmanı.
* **Authorization Code Grant**: İstemcinin gizli anahtarı sızdırmadan tarayıcı üzerinden kod alıp sunucu arkasından token ile takas ettiği güvenli akış.

---

## 10. Kod Örnekleri

### OAuth2 Başarı İşleyici Mantığı (`OAuth2LoginSuccessHandler.java`)
```java
@Override
public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
    OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

    String email = oauth2User.getAttribute("email");
    String providerId = oauth2User.getAttribute("sub");
    String name = oauth2User.getAttribute("name");

    Employee employee = repository.findByEmail(email)
            .map(existing -> linkGoogleIfNeeded(existing, providerId))
            .orElseGet(() -> registerNewOAuthEmployee(email, providerId, name));

    String accessToken = jwtUtil.generateToken(employee);
    RefreshToken refreshToken = refreshTokenService.createRefreshToken(employee.getUsername());

    // JSON veya Redirect yanıtı dönülür
}
```

---

## 11. API Açıklamaları

| Method | Endpoint | Açıklama |
| :--- | :--- | :--- |
| **GET** | `/oauth2/authorization/google` | Kullanıcıyı Google oturum açma sayfasına yönlendirir. |
| **GET** | `/login/oauth2/code/google` | Google callback adresi. OAuth2 koda karşılık JWT dönülür. |

---

## 12. Kurulum

1. Google Cloud Console üzerinde proje oluşturun.
2. OAuth 2.0 Client Credentials oluşturup `Redirect URI` olarak `http://localhost:8080/login/oauth2/code/google` ekleyin.
3. `application.properties` dosyasına ekleyin:
   ```properties
   spring.security.oauth2.client.registration.google.client-id=YOUR_CLIENT_ID
   spring.security.oauth2.client.registration.google.client-secret=YOUR_CLIENT_SECRET
   spring.security.oauth2.client.registration.google.scope=email,profile
   ```

---

## 13. Konfigürasyon

* `SecurityConfig` içerisine `http.oauth2Login(oauth2 -> oauth2.successHandler(oAuth2LoginSuccessHandler))` eklenmiştir.

---

## 14. Güvenlik

* Şifresi bulunmayan OAuth kullanıcılarının (`passwordHash = null`) yerel şifreli giriş yapması engellenmiştir.
* Google tarafından doğrulanmamış e-postalar (`email_verified == false`) reddedilir.

---

## 15. Veri Akışı

```
Google Login -> Redirect -> Auth Code -> Token Exchange -> OAuth2User -> Provision DB -> JWT Issued
```

---

## 16. Hata Yönetimi

* E-posta veya onay eksikse `400 Bad Request` yanıtı üretilir.

---

## 17. Performans

* Harici HTTP çağrısı sadece login anında Google ile gerçekleşir. Sonraki istekler yerel JWT ile statik yürür.

---

## 18. Geliştirici Notları

* Üretim ortamında OAuth2 sonrası istemciye token dönüşü doğrudan JSON yerine güvenli Frontend Redirect URL (`http://frontend.com/oauth2/redirect?token=...`) ile yapılmalıdır.

---

## 19. Gelecekte Yapılabilecek Geliştirmeler

- [ ] GitHub / Facebook Social Login entegrasyonları.
- [ ] Frontend OAuth2 Redirect URI konfigürasyonu.
