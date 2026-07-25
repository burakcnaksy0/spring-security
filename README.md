# 🛡️ Enterprise Spring Security & JWT Core Architecture (`master` Branch)

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

Bu proje, modern kurumsal Java uygulamalarında **Spring Boot 3.x/4.x** ve **Spring Security 6.x** standartları kullanılarak sıfırdan inşa edilmiş, durumsuz (stateless) **JWT (JSON Web Token)** tabanlı kimlik doğrulama (Authentication) ve rol tabanlı yetkilendirme (Authorization / RBAC) altyapısının **temel çekirdek mimarisidir (`master` branch)**.

### Amaç ve Kapsam
Projenin ana amacı; mikroservis ve modern monoliter mimarilerde güvenli, ölçeklenebilir, veritabanı senkronizasyonlu ve Refresh Token rotasyonuna sahip bir kullanıcı/çalışan yönetim (Employee Management) platformu sunmaktır. Bu repository bünyesindeki tüm diğer gelişmiş güvenlik senaryoları (2FA, OAuth2, CSRF, Token Blacklist, Dynamic DB Rules vb.), bu branch'te tanımlanan temel mimari üzerine inşa edilmiştir.

### Problem ve Çözüm Senaryoları
* **Gerçek Hayat Kullanımı**: İnsan Kaynakları (HR) SaaS sistemleri, B2B Kurumsal Çalışan Portalları veya Yönetim Panelleri (Admin Dashboards).
* **Sağladığı Fayda**: Şifrelerin güvenli hash'lenmesi, oturum durumunun (session state) sunucu belleğini tüketmemesi, her API isteğinde anlık yetki doğrulaması ve Refresh Token sayesinde kesintisiz ve güvenli kullanıcı deneyimi.

---

## 2. Problem Tanımı

Geleneksel web uygulamalarında kimlik doğrulama genellikle HTTP Session (JSESSIONID ve sunucu hafızasında bir `HttpSession` nesnesi) ile yönetilir. Ancak modern kurumsal uygulamalarda bu yaklaşım ciddi problemler yaratır:

1. **Dikey ve Yatay Ölçeklenme Engeli (Scalability Bottleneck)**: Sunucu sayısı arttığında (load balancing arkasında), HTTP Session'ların sunucular arasında senkronize edilmesi (session replication / sticky session) karmaşıktır ve performans kaybına yol açar.
2. **Güvenlik Açıkları (CSRF Riskleri)**: Cookie tabanlı session yönetimi, varsayılan olarak Cross-Site Request Forgery (CSRF) saldırılarına açıktır.
3. **Mobil ve İstemci Çeşitliliği**: Mobil uygulamalar (iOS/Android), SPA istemcileri (React/Angular/Vue) ve 3. parti API entegrasyonları HTTP Session mantığıyla zor entegre olur.
4. **Zayıf Parola ve Yetkisiz Erişim**: Güçsüz parolalar, yetersiz veri doğrulaması ve rollerin merkezi yönetilmemesi durumunda sistem verisi kolayca tahrif edilebilir.

---

## 3. Çözüm Yaklaşımı

Proje, geleneksel Stateful (Session tabanlı) mimari yerine **Stateless RESTful Güvenlik Mimarisi** tercih etmiştir.

### Mimari Kararlar ve Avantajları
* **Stateless JWT Mimarisi**: Sunucu geleneksel session bilgisi tutmaz. Kimlik doğrulama bilgisi dijital olarak imzalanmış token içerisinde taşınır. Bu sayede yatayda sonsuz ölçeklenebilirlik sağlanır.
* **Double-Token Strategy (Access & Refresh Token)**:
  * **Access Token**: 15-30 dakikalık kısa ömürlü token. Ağda çalınsa bile etkisi zamanla kısıtlıdır.
  * **Refresh Token**: Veritabanı (`refresh_tokens` tablosu) ile senkronize çalışan uzun ömürlü token. Access token süresi dolduğunda kullanıcının tekrar şifre girmeden yeni token seti almasını sağlar (Refresh Token Rotation).
* **Veritabanı Senkronize DB Check**: Filtre seviyesinde (`JwtAuthenticationFilter`), token doğrulandıktan sonra veritabanından kullanıcı durumu ve güncel rolleri okunur. Bu tercih pure-stateless token'ların "yetki iptal edilememe" problemini çözer. Kullanıcının hesabı dondurulduğunda veya yetkisi alındığında anında sistemden engellenir.
* **Declarative Validation & Custom Annotations**: Şifre karmaşıklık kuralları standart String doğrulaması yerine custom JSR-380 anotasyonu (`@Password`) ile deklaratif hale getirilmiştir.

---

## 4. Kullanılan Teknolojiler

| Teknoloji | Versiyon / Tür | Kullanım Amacı ve Seçim Sebebi |
| :--- | :--- | :--- |
| **Java** | 17 LTS | Modern dil özellikleri (Records, Pattern Matching, Sealed Classes), yüksek performans ve uzun süreli kurumsal destek. |
| **Spring Boot** | 3.x / 4.x | Otomatik konfigürasyon, gömülü Tomcat sunucusu ve hızlı mikroservis/REST API geliştirme ekosistemi. |
| **Spring Security** | 6.x | Servlet Filter Chain mimarisi ile declarative & programmatic endpoint koruması, RBAC ve password encoder entegrasyonu. |
| **JJWT (io.jsonwebtoken)** | 0.12.7 | Java ortamında cryptographic HMAC-SHA (HS256/HS512) ile JWT üretimi, ayrıştırılması (parsing) ve imza doğrulaması. |
| **Spring Data JPA / Hibernate** | Starter | ORM (Object-Relational Mapping) katmanı. Veritabanı bağımsız JPQL/Criteria query yönetimi ve entity ilişkileri. |
| **MySQL Connector** | Runtime | MySQL veritabanı sürücüsü. Üretim ortamlarında ilişkisel veri depolama desteği. |
| **SpringDoc OpenAPI** | 2.8.9 | Swagger-UI entegrasyonu. REST API'lerin otomatik canlı dokümantasyonunun üretilmesi ve UI üzerinden test edilebilmesi. |
| **Lombok** | 1.x / Starter | `@Getter`, `@Setter`, `@Builder`, `@RequiredArgsConstructor` anotasyonları ile boilerplate (tekrarlayan) kodların elenmesi. |
| **Custom Password Validation** | JSR-303 / 380 | Şifre güvenliği politikalarını anotasyon tabanlı olarak merkezi yönetmek. |

---

## 5. Proje Mimarisi

Sistem katmanlı mimari (Layered Architecture) ve Spring Security Servlet Filter Chain üzerine kurulmuştur.

### Mimari Katmanlar
```
[ Client Request ]
       │
       ▼
[ Spring Security Filter Chain ] ──► (JwtAuthenticationFilter)
       │                                     │
       ▼                                     ▼
[ Controller Katmanı ] (AuthController, EmployeeController)
       │
       ▼
[ Service Katmanı ] (AuthService, EmployeeService, RefreshTokenService)
       │
       ▼
[ Repository Katmanı ] (EmployeeRepository, RefreshTokenRepository)
       │
       ▼
[ Database ] (MySQL Engine)
```

### Request Lifecycle & Filter Chain Flow
1. İstemci HTTP başlığında `Authorization: Bearer <Access-Token>` ekleyerek istek atar.
2. İstek Spring Security `SecurityFilterChain` yapısına girer.
3. `JwtAuthenticationFilter` devreye girer:
   - Header içerisindeki `Bearer` prefix'i kontrol edilir.
   - `JwtUtil` ile token imzası ve `expiration` doğrulanır.
   - Token içerisinden `username` çıkarılır.
   - `CustomUserDetailsService.loadUserByUsername()` çağrılarak veritabanından güncel `Employee` entity'si okunur.
   - `CustomUserPrincipal` oluşturulur ve `UsernamePasswordAuthenticationToken` ile Spring `SecurityContextHolder` içerisine set edilir.
4. İstek yetkilendirme süzgecinden (`hasRole('ADMIN')` vs.) geçer.
5. Uygun Controller metoduna erişilir, Service iş mantığını çalıştırır, DTO dönüşümü yapılır ve JSON yanıtı dönülür.

---

## 6. Klasör Yapısı

```
src/main/java/com/burakcanaksoy/springsecurity/
├── SpringSecurityApplication.java      # Spring Boot Ana Başlatıcı Sınıfı
├── annotation/                          # Özel Anotasyonlar
│   └── Password.java                   # Özel Şifre Doğrulama Anotasyonu (@Password)
├── config/                              # Konfigürasyon Sınıfları
│   ├── JwtConfig.java                  # JWT Secret Key ve Expiration değerleri bean'i
│   ├── SecurityConfig.java             # Spring Security Filter Chain & PasswordEncoder Bean'leri
│   └── SwaggerConfig.java              # OpenAPI Swagger UI Güvenlik Ayarları
├── controller/                          # REST Controller Katmanı
│   ├── AuthController.java             # Login, Register, Refresh Token, Logout Uç Noktaları
│   └── EmployeeController.java         # Çalışan CRUD ve Profil Yönetimi Uç Noktaları
├── dto/                                 # Data Transfer Objects (Request/Response)
│   ├── request/                        # İstemciden Gelen İstek Verileri
│   │   ├── EmployeeCreateRequest.java
│   │   ├── EmployeeLoginRequest.java
│   │   ├── EmployeeRegisterRequest.java
│   │   └── RefreshTokenRequest.java
│   └── response/                       # İstemciye Dönen Yanıt Verileri
│       ├── AuthResponse.java
│       ├── EmployeeResponse.java
│       ├── LoginResponse.java
│       └── RefreshTokenResponse.java
├── entity/                              # JPA Veritabanı Entity'leri
│   ├── Employee.java                   # Kullanıcı/Çalışan Entity'si
│   ├── RefreshToken.java               # Refresh Token Veritabanı Kaydı
│   └── Role.java                       # USER, ADMIN Enum Rolleri
├── exception/                           # İstisna Yönetimi ve Custom Exception'lar
│   ├── AlreadyExistsException.java     # Çift Kayıt İstisnası (409 Conflict / 400 Bad Request)
│   ├── GlobalExceptionHandler.java     # Central Controller Advice (@RestControllerAdvice)
│   └── ResourceNotFoundException.java  # Bulunamadı İstisnası (404 Not Found)
├── filter/                              # Custom Servlet Filtreleri
│   └── JwtAuthenticationFilter.java   # HTTP İsteklerinde Token Doğrulama Filtresi
├── mapper/                              # Entity <-> DTO Dönüştürücüler
│   └── EmployeeMapper.java             # Manual/Helper Dönüşüm Metotları
├── repository/                          # Spring Data JPA Repository Arabirimleri
│   ├── EmployeeRepository.java
│   └── RefreshTokenRepository.java
├── security/                            # Spring Security Çekirdek Bileşenleri
│   ├── CustomUserDetailsService.java   # Spring Security UserDetails Yükleyici
│   ├── CustomUserPrincipal.java        # UserDetails Implementasyonu
│   ├── JwtAccessDeniedHandler.java     # 403 Forbidden İşleyicisi
│   └── JwtAuthenticationEntryPoint.java# 401 Unauthorized İşleyicisi
├── service/                             # İş Mantığı Katmanı (Business Logic)
│   ├── AuthService.java                # Authentication, Token Üretimi, Logout
│   ├── EmployeeService.java            # Çalışan Yönetim İş Mantığı
│   └── RefreshTokenService.java        # Refresh Token Rotasyon ve Silme İşlemleri
├── util/                                # Yardımcı Araçlar
│   └── JwtUtil.java                    # JWT Token Üretme, Parsing ve İmza Kontrolü
└── validator/                           # Custom Validator Implementasyonları
    └── PasswordValidation.java         # @Password Anotasyonu Doğrulama Mantığı
```

---

## 7. Kodun Genel Akışı

### 1. Kullanıcı Kayıt Akışı (`/api/v1/auth/register`)
- Client `EmployeeRegisterRequest` JSON verisini gönderir.
- `GlobalExceptionHandler` ve Validation katmanı (`@Valid`, `@Password`) request DTO'sunu denetler.
- `AuthService.register()` çağrılır.
- `EmployeeRepository` üzerinden e-posta, kullanıcı adı ve TC No benzersizliği kontrol edilir. Çakışma varsa `AlreadyExistsException` fırlatılır.
- Şifre `PasswordEncoder.encode()` (BCrypt) ile hash'lenir.
- Rol olarak varsayılan `ROLE_USER` atanır ve DB'ye kaydedilir.

### 2. Login Akışı (`/api/v1/auth/login`)
- Client `EmployeeLoginRequest` gönderir.
- `AuthService.login()` çağrılır.
- `AuthenticationManager.authenticate()` üzerinden kullanıcı adı ve şifre doğrulanır.
- Doğrulama başarılı ise `JwtUtil.generateToken()` ile Access Token üretilir.
- `RefreshTokenService.createRefreshToken()` ile veritabanına yeni bir Refresh Token kaydedilir.
- `LoginResponse` DTO'su ile token'lar istemciye döner.

### 3. Korumalı İstek Akışı (Örn: `GET /api/v1/employees/all`)
- Client `Authorization: Bearer <Token>` başlığı ekler.
- `JwtAuthenticationFilter` isteği yakalar, token'ı doğrular ve `SecurityContextHolder`'a `CustomUserPrincipal` yükler.
- `SecurityConfig` üzerindeki `.hasRole("ADMIN")` kuralı kontrol edilir.
- Kullanıcı `ADMIN` rolüne sahipse `EmployeeController.getAllEmployees()` metoduna ulaşır.
- Yanıt olarak çalışan listesi döner.

---

## 8. Önemli Sınıflar

| Sınıf Adı | Sorumluluğu ve Görevi |
| :--- | :--- |
| **`SecurityConfig`** | Spring Security filter chain'ini yapılandırır. CSRF'yi kapatır, Session politikasını `STATELESS` yapar, endpoint erişim kurallarını ve exception handler'ları bağlar. |
| **`JwtAuthenticationFilter`** | `OncePerRequestFilter` sınıfından türetilmiştir. Her HTTP isteğinde bir kez çalışarak JWT token'ı doğrular ve Spring Security context'ine authentication objesini koyar. |
| **`JwtUtil`** | HMAC-SHA256 kullanarak token imzalama, claims ekleme (`role`, `username`), süre (expiration) kontrolü yapma gibi cryptographic işlemleri yürütür. |
| **`AuthService`** | Login, register, token yenileme ve logout süreçlerini orkestre eder. |
| **`CustomUserDetailsService`** | Spring Security'nin veritabanından kullanıcı verisi okuma kontratını (`loadUserByUsername`) `EmployeeRepository` kullanarak bağlar. |
| **`CustomUserPrincipal`** | `UserDetails` arabirimini implemente eder. Kullanıcı kimliğini, şifresini ve yetkilerini (`GrantedAuthority`) Spring Security'ye temsil eder. |
| **`GlobalExceptionHandler`** | Projedeki tüm runtime exception'ları yakalayarak istemciye tutarlı ve anlamlı bir JSON hata formatı sunar. |

---

## 9. Önemli Teknik Kavramlar

* **Dependency Injection (DI) & IoC**: Spring konteynerinin nesne bağımlılıklarını sınıfın kendi içerisinden oluşturması yerine dışarıdan (constructor üzerinden) enjekte etmesidir.
* **Spring Bean**: Spring IoC konteyneri tarafından yönetilen, yaşam döngüsü Spring tarafından kontrol edilen nesnelerdir (`@Service`, `@Repository`, `@Component`, `@Bean`).
* **JWT (JSON Web Token)**: İki taraf arasında verilerin güvenli ve dijital olarak imzalanmış şekilde JSON formatında aktarılmasını sağlayan RFC 7519 standardı.
* **Filter Chain (Filtre Zinciri)**: HTTP isteklerinin servlet controller'a ulaşmadan önce güvenlik, loglama, başlık denetimi gibi süzgeçlerden sırayla geçmesini sağlayan mekanizma.
* **SecurityContextHolder**: Spring Security'de o anki thread (istek) bazında kimliği doğrulanmış kullanıcının (`Authentication`) tutulduğu thread-local depolama alanı.
* **BCrypt**: Şifre hash'leme işleminde kullanılan, "salt" içeren ve gömülü maliyet faktörü (cost factor) sayesinde brute-force saldırılarına dayanıklı algoritma.
* **DTO (Data Transfer Object)**: Veritabanı Entity nesnelerini dış dünyaya açmak yerine sadece gerekli alanları taşımak amacıyla kullanılan veri transfer nesnesi.

---

## 10. Kod Örnekleri

### Security Filter Chain Yapılandırması (`SecurityConfig.java`)
```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(exception -> exception
            .authenticationEntryPoint(jwtAuthenticationEntryPoint)
            .accessDeniedHandler(jwtAccessDeniedHandler)
        )
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/v1/auth/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
            .requestMatchers("/api/v1/employees/all").hasRole("ADMIN")
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
}
```
> **Açıklama**: Session durumu tamamen kaldırılmış (`STATELESS`), `/auth/**` ve Swagger endpoint'leri herkese açılmış, çalışan listeleme sadece `ADMIN` yetkisine bağlanmış ve custom `JwtAuthenticationFilter` Spring'in standart filtreleme sırasına yerleştirilmiştir.

---

## 11. API Açıklamaları

### Auth Uç Noktaları (`/api/v1/auth`)

| Method | Endpoint | Erişim | Açıklama |
| :--- | :--- | :--- | :--- |
| **POST** | `/register` | Public | Yeni kullanıcı/çalışan kaydı. |
| **POST** | `/login` | Public | Kullanıcı girişi yapar; Access ve Refresh Token döner. |
| **POST** | `/refresh` | Public | Geçerli Refresh Token ile yeni Access Token üretir. |
| **POST** | `/logout` | Authenticated | Kullanıcının aktif refresh token'ını veritabanından siler. |

### Employee Uç Noktaları (`/api/v1/employees`)

| Method | Endpoint | Erişim | Açıklama |
| :--- | :--- | :--- | :--- |
| **GET** | `/all` | `ROLE_ADMIN` | Sistemdeki tüm çalışanları listeler. |
| **DELETE** | `/{id}` | `ROLE_ADMIN` | Belirtilen ID'ye sahip çalışanı siler. |
| **GET** | `/` | Authenticated | Oturum açan kullanıcının kendi profilini getirir. |
| **PUT** | `/` | Authenticated | Oturum açan kullanıcının profil bilgilerini günceller. |

---

## 12. Kurulum

### Gereksinimler
- Java 17+ JDK
- Maven 3.8+
- MySQL Server 8.0+

### Adım Adım Çalıştırma

1. **Veritabanını Oluşturun**:
   ```sql
   CREATE DATABASE spring_security_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
2. **Projeyi Derleyin**:
   ```bash
   mvn clean package -DskipTests
   ```
3. **Uygulamayı Çalıştırın**:
   ```bash
   mvn spring-boot:run
   ```
4. **Swagger UI Adresi**:
   ```
   http://localhost:8080/swagger-ui.html
   ```

---

## 13. Konfigürasyon

`application.properties` dosyası üzerinden veritabanı ve JWT ayarları yapılmaktadır:

```properties
spring.application.name=spring-security
server.port=8080

# Veritabanı Bağlantısı
spring.datasource.url=jdbc:mysql://localhost:3306/spring_security_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:root}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA / Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# JWT Konfigürasyonu
jwt.secret=${JWT_SECRET:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}
jwt.expiration=900000 # 15 Dakika (milisaniye cinsinden)
jwt.refresh-expiration=604800000 # 7 Gün (milisaniye cinsinden)
```

---

## 14. Güvenlik

1. **BCrypt Password Hashing**: Şifreler düz metin (plaintext) olarak saklanmaz, salting mantığı içeren BCrypt ile güvenli biçimde şifrelenir.
2. **Stateless Session Management**: HTTP Session kullanılmadığı için Session Hijacking (Oturum Çalma) riski elenmiştir.
3. **Refresh Token Rotation & Revocation**: Refresh Token DB'de tutulur ve silinebilir (`/logout`). Ayrıca her `/refresh` isteğinde eski token silinip yeni token üretilerek token hırsızlığı riski azaltılır.
4. **Custom Exception Koruması**: Güvenlik hatalarında duyarlı veriler (stack trace) gizlenerek sadece standart HTTP statü kodları (401, 403) ve sade JSON hata mesajları dönülür.

---

## 15. Veri Akışı

```
Client (JSON Payload) 
  ──► DTO Validation (@Valid, @Password) 
  ──► Controller (Request Mapping) 
  ──► Service Layer (Business Logic & Password Hashing) 
  ──► Mapper (DTO -> Entity) 
  ──► Repository (JPA Save) 
  ──► Database (MySQL Record)
```

---

## 16. Hata Yönetimi

`GlobalExceptionHandler` sınıfı `@RestControllerAdvice` ile tüm exception'ları yakalar:

* **`MethodArgumentNotValidException`**: Form validation hatalarında `400 Bad Request` ve hangi alanların hatalı olduğunu belirten detaylı map döner.
* **`AlreadyExistsException`**: Kullanıcı adı/email çakışmasında `400 Bad Request` / `409 Conflict` döner.
* **`ResourceNotFoundException`**: Aranan veri bulunamadığında `404 Not Found` döner.
* **`AccessDeniedException`**: Yetkisiz erişimlerde `403 Forbidden` döner.

---

## 17. Performans

* **Stateless JWT**: Sunucu belleğinde milyonlarca aktif oturum nesnesi saklanmadığı için RAM kullanımı düşüktür.
* **HikariCP Connection Pool**: Spring Boot varsayılan veritabanı bağlantı havuzu olan HikariCP ile hızlı veritabanı bağlantı yönetimi sağlanır.
* **İyileştirme Önerisi**: Sık kullanılan `Employee` yetki sorguları için Redis önbellekleme (Caching) entegre edilebilir.

---

## 18. Geliştirici Notları

* Yeni bir korumalı endpoint eklerken `SecurityConfig` içerisindeki `requestMatchers` kurallarına ekleme yapmayı veya metod seviyesinde `@PreAuthorize` anotasyonu eklemeyi unutmayın.
* Entity nesnelerini kesinlikle doğrudan Controller katmanında dış dünyaya açmayın; daima DTO (`Request`/`Response`) sınıfları kullanın.
* Şifre güncellemelerinde `@Password` anotasyonu kurallarına riayet edin.

---

## 19. Gelecekte Yapılabilecek Geliştirmeler

- [ ] JWT Token'larının sunucu tarafında anlık iptali için **Redis Token Blacklist** mimarisi.
- [ ] İki Aşamalı Doğrulama (**2FA Email / Google Authenticator**) entegrasyonu.
- [ ] E-posta Adresi Doğrulama ve Şifre Sıfırlama Token yapıları.
- [ ] Cookie tabanlı CSRF Koruması.
- [ ] Rate-Limiting (İstek Sınırlama) koruması.

*(Not: Yukarıdaki tüm geliştirmeler bu projenin diğer ilgili özellik branch'lerinde adım adım uygulanmıştır.)*
