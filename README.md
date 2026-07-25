# 🍪 HttpOnly Cookie & CSRF Security Architecture (`cookie-csrf-security` Branch)

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

Bu branch, JWT token'larının istemci tarafında **LocalStorage / SessionStorage yerine HttpOnly, Secure ve SameSite=Strict HTTP Çerezlerinde (Cookies)** saklandığı ve buna bağlı olarak ortaya çıkan **CSRF (Cross-Site Request Forgery)** tehdidine karşı Spring Security `CookieCsrfTokenRepository` ile koruma sağlayan güvenlik mimarisidir.

### Amaç ve Kapsam
XSS (Cross-Site Scripting) saldırılarının JWT token'larını çalmasını tamamen engellemek ve çerez kullanımından doğan CSRF riskini Double-Submit Cookie deseniyle bertaraf etmektir.

---

## 2. Problem Tanımı

1. **LocalStorage / SessionStorage Güvenlik Açığı (XSS Risk)**: JWT token'ı JavaScript bellek veya LocalStorage'a yazıldığında, sitedeki tek bir XSS (zararlı script) açık ile `localStorage.getItem('token')` çağrılarak tüm oturum çalınabilir.
2. **Cookie Otomatik Gönderim Riski (CSRF Risk)**: Token çereze koyulduğunda tarayıcı bunu isteklerde otomatik gönderir. Kullanıcı zararlı bir siteye girdiğinde, zararlı site kullanıcının haberi olmadan `POST /employees/delete/1` isteği atabilir.

---

## 3. Çözüm Yaklaşımı

* **HttpOnly Cookies (`CookieUtil`)**: Token'lar `Set-Cookie` başlığı ile yanıt olarak dönülür ve `httpOnly(true)` bayrağı eklenir. JavaScript (XSS) kesinlikle çerez içeriğini okuyamaz.
* **SameSite=Strict**: Çerezlerin sadece uygulamanın kendi domain'inden yapılan isteklerde gönderilmesi sağlanır.
* **Double-Submit Cookie CSRF Protection**: Spring Security `CookieCsrfTokenRepository.withHttpOnlyFalse()` kullanılarak `XSRF-TOKEN` çerezi oluşturulur. İstemci (React/Angular) bu token'ı okuyup HTTP header'ına (`X-XSRF-TOKEN`) eklemek zorundadır. Sunucu header ile cookie değerini eşleştirir.
* **Path-Based Cookie Scoping**: Refresh Token çerezinin yolu sadece `/api/v1/auth/refresh` olarak kısıtlanmıştır; genel isteklere boşa eklenmez.

---

## 4. Kullanılan Teknolojiler

| Teknoloji | Kullanım Amacı |
| :--- | :--- |
| **Spring Security CSRF Module** | `CookieCsrfTokenRepository` ve `CsrfTokenRequestAttributeHandler` entegrasyonu. |
| **ResponseCookie (Spring Web)** | `HttpOnly`, `SameSite`, `Path`, `MaxAge` özellikli kurumsal çerez üretimi. |
| **CorsConfigurationSource** | Cookie tabanlı isteklerde `allowCredentials(true)` ve CORS Origin yapılandırması. |

---

## 5. Proje Mimarisi

```
[ Login Request ] ──► AuthService ──► Set-Cookie: accessToken (HttpOnly, SameSite=Strict)
                                   ──► Set-Cookie: XSRF-TOKEN (HttpOnly=False)

[ Mutating Request (POST/PUT/DELETE) ] 
       │
       ▼
Client Reads XSRF-TOKEN Cookie via JS ──► Adds Header "X-XSRF-TOKEN: <value>"
       │
       ▼
[ Spring Security CsrfFilter ] ──► Compares Header vs Cookie Value
       │
       ▼ (If Valid)
[ JwtAuthenticationFilter ] ──► Reads Access Token from HttpOnly Cookie ──► Authenticates User
```

---

## 6. Klasör Yapısı

```
src/main/java/com/burakcanaksoy/springsecurity/
├── util/
│   └── CookieUtil.java                 # Access & Refresh Token için HttpOnly Cookie üretme ve temizleme
├── filter/
│   └── CsrfCookieFilter.java           # Lazy CSRF Token yüklemesini tetikleyen süzgeç
├── config/
│   ├── SecurityConfig.java             # CookieCsrfTokenRepository ve Ignoring URL yapılandırması
│   └── CorsConfig.java                 # Allowed Origins ve allowCredentials(true) ayarı
```

---

## 7. Kodun Genel Akışı

1. Kullanıcı `/login` olur. `CookieUtil` yardımıyla `accessToken` ve `refreshToken` HttpOnly çerez olarak HTTP cevabına eklenir.
2. Spring Security `CookieCsrfTokenRepository` tarayıcıya readable `XSRF-TOKEN` çerezi bırakır.
3. İstemci veriyi değiştiren bir istek (POST/PUT/DELETE) yaparken `X-XSRF-TOKEN` header'ını ekler.
4. `CsrfCookieFilter` ve Spring Security CSRF filtresi header ile çerezi karşılaştırır.
5. `JwtAuthenticationFilter` isteğin çerezlerinden `accessToken`'ı okur ve kullanıcının kimliğini doğrular.

---

## 8. Önemli Sınıflar

* **`CookieUtil`**: `ResponseCookie` kullanarak `httpOnly(true)`, `sameSite("Strict")` ve özel path tanımlı çerezler oluşturan utility sınıfı.
* **`CsrfCookieFilter`**: Spring Security 6'daki deferred CSRF token mekanizmasını otomatik initialize eden `OncePerRequestFilter`.
* **`CorsConfig`**: Çerezli isteklerin (Credentials) farklı domain'lerden kabul edilmesini düzenleyen CORS konfigürasyonu.

---

## 9. Önemli Teknik Kavramlar

* **HttpOnly Cookie**: Tarayıcıda `document.cookie` komutuyla JavaScript tarafından okunması engellenmiş, sadece HTTP isteklerinde otomatik taşınan çerez türü.
* **CSRF (Cross-Site Request Forgery)**: Kullanıcının oturum açtığı bir sitedeki yetkilerini, kullanıcının haberi olmadan başka bir zararlı site üzerinden kötüye kullanma saldırısı.
* **SameSite Attribute**: Çerezlerin üçüncü taraf (cross-site) isteklerde gönderilip gönderilmeyeceğini belirleyen tarayıcı güvenlik politikasısı (`Strict`, `Lax`, `None`).

---

## 10. Kod Örnekleri

### HttpOnly Cookie Üretimi (`CookieUtil.java`)
```java
public void addAccessTokenCookie(HttpServletResponse response, String token, long maxAgeSeconds) {
    ResponseCookie cookie = ResponseCookie.from(ACCESS_TOKEN_COOKIE, token)
            .httpOnly(true)
            .secure(false) // Production ortamında true (HTTPS) olmalıdır
            .sameSite("Strict")
            .path("/")
            .maxAge(maxAgeSeconds)
            .build();
    response.addHeader("Set-Cookie", cookie.toString());
}
```

---

## 11. API Açıklamaları

* Giriş yapıldığında JSON Body yerine HTTP Headers kısmında `Set-Cookie` bilgileri yer alır.
* CSRF Koruması aktif olan tüm değiştiren (state-changing) isteklere `X-XSRF-TOKEN` header'ı eklenmelidir.

---

## 12. Kurulum

1. Projeyi çalıştırın:
   ```bash
   mvn spring-boot:run
   ```
2. Postman veya frontend entegrasyonunda "Enable Cookie Jar" seçeneğini aktif edin.

---

## 13. Konfigürasyon

`SecurityConfig` içerisinde CSRF'ten muaf tutulan (public login/register) uç noktalar tanımlanmıştır.

---

## 14. Güvenlik

* **XSS Tam Koruması**: JavaScript kodları token'a erişemeyeceği için zararlı kodlar token çalamaz.
* **Production Notu**: `secure(false)` ifadesi üretim ortamında HTTPS kullanımıyla kesinlikle `secure(true)` olarak güncellenmelidir.

---

## 15. Veri Akışı

```
Login -> HTTP Set-Cookie (HttpOnly) -> Request with Credentials -> CsrfFilter Check -> JwtCookie Extract -> Service Execution
```

---

## 16. Hata Yönetimi

* CSRF Token eksik veya uyuşmuyorsa Spring Security `403 Forbidden (Invalid CSRF Token)` hatası döner.

---

## 17. Performans

* Header bazlı token aktarımına kıyasla ağ yükü ve performans başarımında gözle görülür bir fark bulunmamaktadır.

---

## 18. Geliştirici Notları

* Frontend uygulamasında (axios/fetch) `withCredentials: true` ayarı açılmalıdır.

---

## 19. Gelecekte Yapılabilecek Geliştirmeler

- [ ] CSRF Token yenileme rotasyonu.
- [ ] Subdomain'ler arası Cookie Paylaşımı (Domain Scoping).
