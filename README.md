# 🛑 Distributed Rate Limiting Architecture (`rate-limiting` Branch)

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

Bu branch, kritik güvenlik uç noktalarını (Login, OTP, Password Reset, Register) DoS/DDoS ve Brute-Force (Kaba Kuvvet) saldırılarına karşı korumak amacıyla **Redis Dağıtık Destekli Bucket4j Rate Limiting (İstek Sınırlama)** mimarisini uygulamaktadır.

### Amaç ve Kapsam
Belirli bir zaman diliminde (örneğin 1 dakikada max 5 istek) tek bir IP adresinin atabileceği istek sayısını sınırlamak, aşım durumunda HTTP `429 Too Many Requests` döndürmek ve bu sınırı mikroservis/kümeleme (cluster) ortamında Redis üzerinde merkezi olarak yönetmektir.

---

## 2. Problem Tanımı

1. **Brute-Force (Kaba Kuvvet) Saldırıları**: Saldırganların otomatik botlarla `/login` uç noktasına milyonlarca şifre denemesi yapması.
2. **Resource Exhaustion (Kaynak Tüketimi)**: Aşırı istekler sonucu CPU, Veritabanı bağlantıları ve SMTP mail gönderim kotalarının tükenmesi.
3. **Multi-Instance Inconsistency**: In-Memory (Heap) sınırlamaların çoklu sunucu (Load Balancer arkasında 3-5 pod) çalışırken yetersiz kalması.

---

## 3. Çözüm Yaklaşımı

* **Token Bucket Algoritması (Bucket4j)**: Her IP ve path kombinasyonu için içi token ile dolu bir "kova" oluşturulur. İstek geldikçe token harcanır. Kova boşsa istek engellenir.
* **Redis Lettuce Proxy Manager (`LettuceBasedProxyManager`)**: Kovaların durumu tek bir JVM belleğinde değil, kümedeki tüm sunucuların erişebildiği merkezi Redis veritabanında saklanır.
* **Endpoint Pattern Rules (`RateLimitRule`)**: `/api/v1/auth/login` için ayrı, `/forgot-password` için ayrı kapasite ve doldurma (refill) süreleri tanımlanabilir.
* **Fail-Open Mimarisi**: Redis sunucusu çökse dahi sistemin çalışmaya devam etmesi için exception try-catch ile sarmalanır; rate limiter devre dışı kalır ama API durmaz.

---

## 4. Kullanılan Teknolojiler

| Teknoloji | Kullanım Amacı |
| :--- | :--- |
| **Bucket4j (8.x)** | Java Token Bucket rate limiting algoritması uygulaması. |
| **Bucket4j Redis (Lettuce)** | Bucket durumlarını Redis CAS (Compare-And-Swap) ile merkezi saklama. |
| **Spring Data Redis / Lettuce** | Redis sunucu bağlantısı ve client yönetimi. |

---

## 5. Proje Mimarisi

```
[ Client HTTP Request ] 
       │
       ▼
[ RateLimitFilter ] ──► Match Request URI with RateLimitRule Pattern
       │
       ├── (Rule Matched)
       ▼
Generate Key: "rate-limit:/api/v1/auth/login:192.168.1.1"
       │
       ▼
[ Redis Proxy Manager ] ──► Fetch/TryConsume Token in Redis
       │
       ├── Token Available   ──► Set X-RateLimit-* Headers ──► Proceed to Controller
       └── Token Empty       ──► Return HTTP 429 Too Many Requests + Retry-After Header
```

---

## 6. Klasör Yapısı

```
src/main/java/com/burakcanaksoy/springsecurity/
├── filter/
│   └── RateLimitFilter.java            # Bucket4j + Redis tabanlı HTTP istek sınırlama filtresi
├── config/
│   ├── RedisRateLimitConfig.java       # Redis Client ve ProxyManager Bean tanımları
│   └── RateLimitRulesConfig.java       # Path bazlı RateLimitRule kuralları listesi
└── rule/
    └── RateLimitRule.java              # Path, Capacity, RefillDuration kural sınıfı
```

---

## 7. Kodun Genel Akışı

1. İstek `RateLimitFilter` süzgecine girer.
2. `AntPathMatcher` ile isteğin URL'si kurallar listesindeki pattern'lerle eşleştirilir.
3. Eşleşme varsa `rate-limit:<path>:<clientIp>` anahtarı oluşturulur.
4. Redis üzerinden Bucket `tryConsumeAndReturnRemaining(1)` çağrılır.
5. İzin verilirse HTTP yanıtına `X-RateLimit-Limit` ve `X-RateLimit-Remaining` eklenerek zincir devam eder. Aşılırsa `429` döner.

---

## 8. Önemli Sınıflar

* **`RateLimitFilter`**: Redis bağlantılı Bucket4j filtresi.
* **`RedisRateLimitConfig`**: Lettuce `RedisClient` ve Bucket4j `LettuceBasedProxyManager` nesnesini oluşturan konfigürasyon.
* **`RateLimitRule`**: Her endpoint için kapasite (`capacity`) ve dolum süresini (`refillDuration`) belirten model.

---

## 9. Önemli Teknik Kavramlar

* **Token Bucket Algoritması**: Sabit kapasiteli bir kovaya belirli aralıklarla token eklenmesi ve isteklerin token tüketerek geçmesi esasına dayanan algoritma.
* **HTTP 429 Too Many Requests**: İstemcinin verilen süre kısıtlamasında çok fazla istek attığını belirten standart HTTP yanıt kodu.
* **Fail-Open Strategy**: Altyapı bileşeni (Redis) çöktüğünde sistemin çökmek yerine güvenlik süzgecini es geçip hizmet vermeye devam etmesi mantığı.

---

## 10. Kod Örnekleri

### Rate Limit Filter Mantığı (`RateLimitFilter.java`)
```java
Bucket bucket = resolveBucket(rateLimitKey, matchedRule);
ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
long resetSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;

response.setHeader("X-RateLimit-Limit", String.valueOf(matchedRule.getCapacity()));
response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));

if (probe.isConsumed()) {
    filterChain.doFilter(request, response);
} else {
    response.setStatus(429);
    response.setHeader("Retry-After", String.valueOf(resetSeconds));
    response.getWriter().write("{\"message\":\"Too many requests for this action. Please try again later.\"}");
}
```

---

## 11. API Açıklamaları

Korumalı uç noktalara atılan isteklerin yanıt başlıklarında (Headers) şu değerler yer alır:
* `X-RateLimit-Limit`: İzin verilen maksimum istek sayısı.
* `X-RateLimit-Remaining`: Kalan istek sayısı.
* `X-RateLimit-Reset`: Token'ın yeniden dolması için beklenmesi gereken süre (nanos/seconds).

---

## 12. Kurulum

1. Yerel ortamda Redis çalıştırın:
   ```bash
   docker run -d --name redis -p 6379:6379 redis:alpine
   ```
2. `application.properties` dosyasına Redis bağlantısını ekleyin:
   ```properties
   spring.data.redis.host=localhost
   spring.data.redis.port=6379
   ```

---

## 13. Konfigürasyon

`RateLimitRulesConfig` sınıfından endpoint kuralları değiştirilebilir.

---

## 14. Güvenlik

* IP Spoofing (X-Forwarded-For) manipülasyonuna karşı üretim ortamında Reverse Proxy (Nginx/Cloudflare) IP doğrulaması yapılmalıdır.

---

## 15. Veri Akışı

```
Request -> Filter -> Match URI -> Redis CAS Bucket Check -> Token Consumed? -> 200 OK / 429 Too Many Requests
```

---

## 16. Hata Yönetimi

* Limit aşıldığında JSON yanıtı ile `429 Too Many Requests` ve `Retry-After` başlığı dönülür.

---

## 17. Performans

* Redis CAS işlemleri sub-millisecond (1ms altı) yanıt sürelerine sahiptir.

---

## 18. Geliştirici Notları

* Docker Compose ortamlarında Redis bağımlılığının `healthcheck` ile ayağa kalkması sağlanmalıdır.

---

## 19. Gelecekte Yapılabilecek Geliştirmeler

- [ ] IP bazlı erişim engelleme (IP Blacklisting).
- [ ] User ID tabanlı kullanıcı bazlı (Authenticated User Rate Limiting) kısıtlama.
