# 🚫 Redis JWT Token Blacklisting & Revocation Architecture (`token-blacklist` Branch)

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

Bu branch, durumsuz (stateless) JWT mimarisinin en büyük eksikliklerinden biri olan **"Token İptal Edilememe (Immediate Token Revocation / Blacklisting)"** problemini **Redis In-Memory Key-Value Store & JWT JTI Claims** mimarisi ile çözmektedir.

### Amaç ve Kapsam
Kullanıcı `/logout` yaptığında veya bir yöneticinin hesabı anında askıya alması gerektiğinde, süresi henüz dolmamış (valid) Access Token'ların geçerliliğini anında yitirmesini sağlamaktır.

---

## 2. Problem Tanımı

JWT token'lar doğası gereği durumsuzdur (Stateless). İstemciye bir token verildiğinde (örn. 30 dakika süreli), kullanıcı çıkış yapsa bile o token süresi bitene kadar herhangi bir istemci tarafından kullanılabilir.
1. **Çıkış Sonrası Güvenlik Açığı**: Kullanıcı logout dedikten sonra token ağda dinlenmişse (sniffing) süresi dolana kadar sisteme erişilebilir.
2. **Anlık Hesap Dondurma İmkansızlığı**: Şüpheli bir durum fark edildiğinde aktif kullanıcının Access Token'ını anında hükümsüz kılma gereksinimi.

---

## 3. Çözüm Yaklaşımı

* **JWT JTI Claim (RFC 7519)**: Her üretilen Access Token'a `jti` (JWT ID - Unique Identifier) adında benzersiz bir UUID claim eklenir.
* **Redis TTL Blacklist Store (`StringRedisTemplate`)**: Kullanıcı logout olduğunda token'ın `jti` bilgisi ve token'ın kalan ömrü (`remainingValidityMillis`) hesaplanır. Redis'e `blacklist:token:<jti>` anahtarıyla yazılır.
* **Auto-Expiring Clean-Up**: Redis anahtarının yaşam süresi (TTL) token'ın kalan süresine ayarlanır. Token'ın normal süresi dolduğunda Redis anahtarı kendiliğinden silinir (Zero Database Memory Leak).
* **Filter Gatekeeper**: `JwtAuthenticationFilter` gelen her istekte token'ın `jti` değerini Redis'te kontrol eder (`isBlacklisted`).

---

## 4. Kullanılan Teknolojiler

| Teknoloji | Kullanım Amacı |
| :--- | :--- |
| **Redis & StringRedisTemplate** | Millisecond seviyesinde token karaliste kontrolü ve otomatik TTL silme. |
| **JJWT JTI Claim** | Token'ları benzersiz kılan UUID identifiers (`extractJti`). |
| **Spring Boot Data Redis** | Spring ve Redis in-memory veri tabanı entegrasyonu. |

---

## 5. Proje Mimarisi

```
[ Logout Request ] ──► POST /api/v1/auth/logout
                             │
                             ▼
Extract JTI & Remaining Expiration ──► Redis SET "blacklist:token:<jti>" EXPIRE <remaining_ms>

[ Protected API Request ] ──► JwtAuthenticationFilter
                                     │
                                     ▼
Extract JTI ──► Redis HAS_KEY "blacklist:token:<jti>"
                     │
                     ├──► TRUE  ──► Block Request (401 Unauthorized - Blacklisted Token)
                     └──► FALSE ──► Proceed to Controller
```

---

## 6. Klasör Yapısı

```
src/main/java/com/burakcanaksoy/springsecurity/
├── service/
│   └── TokenBlacklistService.java      # Redis Blacklist ekleme (set with TTL) ve kontrol etme (isBlacklisted)
├── util/
│   └── JwtUtil.java                    # JTI üretimi ve extractJti(token) metodu
├── filter/
│   └── JwtAuthenticationFilter.java   # Token karaliste denetim süzgeci
└── controller/
    └── AuthController.java             # Logout isteğinde token'ı karalisteye alma
```

---

## 7. Kodun Genel Akışı

1. Başarılı login sonrası üretilen JWT içine `jti` (UUID) claim eklenir.
2. Kullanıcı `/logout` çağrısı yapar.
3. `TokenBlacklistService.blacklistToken(token, remainingTime)` çağrılır.
4. Redis'e `blacklist:token:<jti> = "true"` yazılır (TTL: kalan milisaniye).
5. İstek atan kullanıcı aynı token ile tekrar erişmek isterse `JwtAuthenticationFilter` engeller (`401`).

---

## 8. Önemli Sınıflar

* **`TokenBlacklistService`**: Redis `StringRedisTemplate` kullanarak token karaliste mantığını yürüten servis.
* **`JwtUtil`**: Token üretirken `setIssuedAt`, `setExpiration` ve `setId(UUID)` ile `jti` gömen utility.

---

## 9. Önemli Teknik Kavramlar

* **JTI (JWT ID)**: JSON Web Token standartlarındaki benzersiz belirteç claim'i.
* **TTL (Time-To-Live)**: Redis'teki bir anahtarın otomatik olarak silineceği süre.
* **Token Revocation**: Verilmiş bir kimlik doğrulama jetonunun süresi dolmadan iptal edilmesi süreci.

---

## 10. Kod Örnekleri

### Token Karaliste Servisi (`TokenBlacklistService.java`)
```java
public void blacklistToken(String jwt, long remainingValidityMillis) {
    if (remainingValidityMillis <= 0) return;
    try {
        String key = key(jwt);
        stringRedisTemplate.opsForValue().set(key, "true", Duration.ofMillis(remainingValidityMillis));
    } catch (Exception e) {
        log.error("Redis connection failed while blacklisting token.", e);
    }
}

public boolean isBlacklisted(String jwt) {
    try {
        String key = key(jwt);
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    } catch (Exception e) {
        log.error("Redis connection failed while checking blacklist.", e);
    }
    return false;
}
```

---

## 11. API Açıklamaları

| Method | Endpoint | Açıklama |
| :--- | :--- | :--- |
| **POST** | `/api/v1/auth/logout` | Aktif Access Token'ı Redis karalistesine alır ve Refresh Token'ı siler. |

---

## 12. Kurulum

1. Redis sunucusunu çalıştırın:
   ```bash
   docker run -d --name redis -p 6379:6379 redis:alpine
   ```
2. Projeyi çalıştırıp `/login` ardından `/logout` yapın ve aynı token ile tekrar korumalı uç noktaya istek atın.

---

## 13. Konfigürasyon

`application.properties`:
```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

---

## 14. Güvenlik

* Karaliste kaydı sadece token'ın kalan ömrü kadar Redis'te tutulur. Token normal süresiyle expired olduğunda Redis kaydı da otomatik biter; hafıza şişmesi engellenir.

---

## 15. Veri Akışı

```
Logout -> Extract JTI -> Redis SET with TTL -> Next Request -> Filter checks Redis -> Reject 401
```

---

## 16. Hata Yönetimi

* Karalistedeki bir token ile istek atıldığında `JwtAuthenticationEntryPoint` `401 Unauthorized` döner.

---

## 17. Performans

* Redis Key Lookup O(1) karmaşıklığındadır (sub-millisecond), isteklere hissedilir bir gecikme eklemez.

---

## 18. Geliştirici Notları

* Redis arızalanması durumunda güvenlik zafiyeti yaşanmaması için varsayılan olarak Fail-Closed veya loglama politikası belirlenmelidir.

---

## 19. Gelecekte Yapılabilecek Geliştirmeler

- [ ] Tüm kullanıcı cihazlarından tek tıkla çıkış (`blacklistAllUserTokens`).
