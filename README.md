# 🛡️ Employee Management & Spring Security JWT API (master Branch)

Bu branch, **Spring Security 6.x** ve **Spring Boot 3.x/4.x** tabanlı, durumsuz (stateless) **JWT (JSON Web Token)** doğrulama mimarisine sahip gelişmiş Çalışan (Employee) Yönetim API'sinin **temel çekirdeğidir (base branch)**. Projedeki diğer tüm özellik dalları (branch'leri), bu temel güvenlik ve kullanıcı yönetim yapısı üzerine kurulmuştur.

---

## 📌 Bu Branch'in Amacı ve Mantığı

`master` branch'inin amacı, uygulamanın kimlik doğrulama (Authentication), yetkilendirme (Authorization/RBAC) ve kullanıcı veritabanı yönetim altyapısını kurmaktır. Güvenlik mimarisi tamamen **durumsuz (Stateless)** olarak tasarlanmıştır; sunucu tarafında HTTP Session tutulmaz. Bunun yerine her istek, beraberinde gönderilen JWT (JSON Web Token) ile doğrulanır.

Bu sürümde temel olarak şu işlevler sağlanmıştır:
1. **Kullanıcı Kaydı & Güvenli Parola Saklama**: Parolalar, **BCrypt** algoritması kullanılarak hash'lenir.
2. **Token Tabanlı Kimlik Doğrulama (Access & Refresh Token)**: Başarılı girişte kısa ömürlü bir JWT (Access Token) ve veritabanı destekli, uzun ömürlü bir Refresh Token üretilir.
3. **Rol Bazlı Yetkilendirme (RBAC)**: Kullanıcılara `USER` veya `ADMIN` rolleri atanarak sistemdeki kaynaklara erişimleri sınırlandırılır.

---

## 🛠️ Bu Branch'te Yapılanlar & Teknik Mimari

### 1. Güvenlik Akışı ve Filtre Zinciri (Filter Chain)
*   **`JwtAuthenticationFilter`**: Gelen her isteğin `Authorization` başlığını inceler. Eğer `Bearer <token>` mevcutsa, token'ın geçerliliğini ve imzasını kontrol eder.
*   **Veritabanı Senkronize Doğrulama**: Filtre, token'dan sadece `username` bilgisini çıkarır ve ardından `CustomUserDetailsService` aracılığıyla veritabanına giderek güncel rolleri/yetkileri sorgular. Bu yaklaşım, kullanıcının hesabı askıya alındığında veya rolleri değiştiğinde sistemin anında (token süresi dolmadan) tepki vermesini sağlar.
*   **`SecurityContextHolder`**: Kimliği başarıyla doğrulanan kullanıcının bilgileri Spring Security context'ine set edilir.

### 2. Çift Token (Access & Refresh Token) Mekanizması
*   **Access Token**: Kısa ömürlüdür (örn. 15-30 dakika). İstekleri doğrulamak için HTTP header üzerinden gönderilir.
*   **Refresh Token**: Uzun ömürlüdür (örn. 7 gün) ve veritabanındaki `RefreshToken` tablosunda saklanır. Access token süresi bittiğinde, kullanıcı adı ve şifre girmeden `/refresh` isteğiyle yeni bir Access token ve yeni bir Refresh token alınmasını sağlar (Refresh Token Rotation).
*   **Güvenli Logout**: Kullanıcı `/logout` isteği yaptığında veritabanındaki aktif `RefreshToken` silinir, böylece çalınma ihtimaline karşı eski token geçersiz kılınır.

### 3. Özel Şifre Validasyonu (`@Password`)
*   Güçlü şifre politikalarını uygulamak için `@Password` anotasyonu ve `PasswordValidation` sınıfı geliştirilmiştir.
*   **Şifre Kuralları:**
    *   En az 8, en fazla 64 karakter.
    *   En az bir büyük harf, bir küçük harf, bir rakam ve bir özel karakter (`@, #, $, %, ^, &, +`).
    *   Boşluk karakteri içeremez.

### 4. İstisna Yönetimi (Exception Handling)
*   **`JwtAuthenticationEntryPoint`**: Kimlik doğrulaması olmaksızın korumalı kaynaklara erişimlerde `401 Unauthorized` şablonu döner.
*   **`JwtAccessDeniedHandler`**: Yetkisi yetersiz olan (örneğin ADMIN sayfasına girmeye çalışan USER) kullanıcılara `403 Forbidden` şablonu döner.
*   **`GlobalExceptionHandler`**: `AccessDeniedException` dahil olmak üzere, `AlreadyExistsException`, `ResourceNotFoundException` ve metot argüman doğrulama hatalarını (`MethodArgumentNotValidException`) yakalayarak standart JSON hata çıktıları üretir.

---

## 🔑 Veritabanı Modeli: `Employee` & `RefreshToken`

### Employee Tablosu
| Alan Adı | Tip | Açıklama |
| :--- | :--- | :--- |
| `id` | Long (PK) | Otomatik artan benzersiz ID |
| `username` | String | Benzersiz kullanıcı adı |
| `passwordHash` | String | BCrypt ile şifrelenmiş parola |
| `firstName` / `lastName` | String | İsim ve Soyisim |
| `tcNo` | String | Benzersiz T.C. Kimlik Numarası |
| `birthDate` | LocalDate | Doğum tarihi |
| `gender` | String | Cinsiyet |
| `phoneNumber` | String | Benzersiz telefon numarası |
| `email` | String | Benzersiz e-posta adresi |
| `address` | String | İkametgah adresi |
| `role` | Enum (Role) | `USER` veya `ADMIN` |

---

## 🗺️ API Uç Noktaları (Endpoints)

### 1. Kimlik Doğrulama Kontrolörü (`AuthController` - `/api/v1/auth/**`)
*Bu endpoint'ler herkese açıktır (`permitAll()`).*

*   **POST** `/register` -> Yeni bir çalışan kaydı oluşturur. Varsayılan olarak `ROLE_USER` yetkisi atanır.
*   **POST** `/login` -> Kullanıcı bilgilerini doğrular ve `accessToken`, `refreshToken` döner.
*   **POST** `/refresh` -> Geçerli bir `refreshToken` ile yeni bir `accessToken` ve yeni bir `refreshToken` üretir.
*   **POST** `/logout` -> Kullanıcının veritabanındaki aktif refresh token'ını silerek oturumu sonlandırır.

### 2. Çalışan Yönetim Kontrolörü (`EmployeeController` - `/api/v1/employees/**`)
*Bu endpoint'ler için kimlik doğrulaması zorunludur.*

*   **GET** `/all` -> Kayıtlı tüm çalışanları listeler (Sadece **ADMIN**).
*   **DELETE** `/{id}` -> Belirtilen ID'deki çalışanı siler (Sadece **ADMIN**).
*   **GET** `/` -> Giriş yapan kullanıcının kendi profil bilgilerini getirir (**USER** veya **ADMIN**).
*   **PUT** `/` -> Giriş yapan kullanıcının profil bilgilerini günceller.

---

## ⚙️ Kurulum ve Çalıştırma

1. **Gereksinimler**: Java 17, MySQL Server.
2. **Yapılandırma**: `application.properties` içerisinde veritabanı ayarlarını ve JWT gizli anahtarını (SECRET) çevre değişkenleri (environment variables) veya doğrudan değerler üzerinden tanımlayın:
   *   `USERNAME`: MySQL kullanıcı adı
   *   `PASSWORD`: MySQL şifresi
   *   `SECRET`: Base64 formatında en az 256-bit JWT imza anahtarı
3. **Derleme ve Çalıştırma**:
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```
