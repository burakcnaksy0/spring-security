# 🔍 Custom Permission Evaluator Architecture (`custom-permission-evaluator` Branch)

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

Bu branch, Spring Security'nin standart `@PreAuthorize("hasRole('ADMIN')")` statik yetkilendirme sınırlarını aşarak, **Nesne Düzeyinde Dinamik İzin Değerlendirme (Object-Level Dynamic Access Control / Domain ACL)** mimarisini `PermissionEvaluator` arabirimi üzerinden uygulamaktadır.

### Amaç ve Kapsam
Örnek olarak eklenen İzin/İzin Talebi (`LeaveRequest`) modülü üzerinde, bir kullanıcının sadece kendi izin talebini okuma/güncelleme/silme yetkisine sahip olduğunu (Ownership), yöneticilerin (ADMIN) ise tüm talepleri onaylama/reddetme (APPROVE/REJECT) yetkisine sahip olduğunu anotasyon seviyesinde dinamik olarak doğrulamaktır.

---

## 2. Problem Tanımı

Geleneksel Rol Tabanlı Erişim Kontrolü (RBAC) sadece kullanıcının rolüne bakar:
* Örneğin `@PreAuthorize("hasRole('USER')")` metodu, herhangi bir `USER` rolüne sahip kişinin sisteme girmiş başka bir kullanıcının ID'sini göndererek verisini okumasına veya silmesine engel olamaz (Insecure Direct Object Reference - IDOR zafiyeti).
* İş mantığını Service katmanına `if (leaveRequest.getEmployee().getId() != currentUser.getId()) throw AccessDeniedException` şeklinde yazmak ise koda bağımlılık ve tekrar getirir.

---

## 3. Çözüm Yaklaşımı

Spring Security'nin `PermissionEvaluator` kontratı kullanılarak **Deklaratif Nesne Düzeyinde Güvenlik** kurulmuştur:
* **CustomPermissionEvaluator**: `hasPermission(Authentication auth, Serializable targetId, String targetType, Object permission)` metodunu implemente eder.
* **Nesne Sahipliği (Ownership Check)**: İsteği atan kullanıcının `CustomUserPrincipal` ID'si ile hedef veritabanı kaydının (`LeaveRequest.employee.id`) eşleşip eşleşmediği kontrol edilir.
* **SpEL Expression Integration**: Controller metodlarında `@PreAuthorize("hasPermission(#id, 'LeaveRequest', 'READ')")` şeklinde doğrudan ID bazlı dinamik sorgular yazılır.

---

## 4. Kullanılan Teknolojiler

| Teknoloji | Kullanım Amacı |
| :--- | :--- |
| **Spring Security Method Security** | `@EnableMethodSecurity` ve SpEL (Spring Expression Language) üzerinden `hasPermission(...)` desteği. |
| **PermissionEvaluator Interface** | Spring Security'nin domain objeleri için özelleştirilebilir yetki değerlendirme arabirimi. |
| **Spring Data JPA & MySQL** | `LeaveRequest` entity'sinin veritabanı ilişkileri ile saklanması. |

---

## 5. Proje Mimarisi

```
[ HTTP GET /api/v1/leave-requests/10 ]
       │
       ▼
[ Spring Security Aspect (@PreAuthorize) ]
       │
       ▼
[ SpEL Evaluator ] ──► (Calls CustomPermissionEvaluator.hasPermission(10, 'LeaveRequest', 'READ'))
       │
       ▼
[ LeaveRequestRepository.findById(10) ]
       │
       ▼
[ Ownership & Role Logic ] ──► (Is User Owner? OR Is User ADMIN?)
       │
       ├──► TRUE  ──► Proceed to LeaveRequestController.getById(10)
       └──► FALSE ──► Throw 403 Forbidden AccessDeniedException
```

---

## 6. Klasör Yapısı

```
src/main/java/com/burakcanaksoy/springsecurity/
├── security/
│   └── CustomPermissionEvaluator.java   # PermissionEvaluator implementasyonu (READ, WRITE, DELETE, APPROVE)
├── controller/
│   └── LeaveRequestController.java     # @PreAuthorize("hasPermission(...)") anotasyonlu REST uç noktaları
├── entity/
│   ├── LeaveRequest.java               # İzin Talebi Entity'si
│   └── enums/
│       ├── Permission.java             # READ, WRITE, DELETE, APPROVE Enum'ları
│       └── LeaveRequestStatus.java     # PENDING, APPROVED, REJECTED Enum'ları
└── service/
    └── LeaveRequestService.java        # İzin talebi iş mantığı
```

---

## 7. Kodun Genel Akışı

1. İstemci `GET /api/v1/leave-requests/5` isteği gönderir.
2. Metot tetiklenmeden önce `@PreAuthorize("hasPermission(#id, 'LeaveRequest', 'READ')")` AOP interceptor'ı devreye girer.
3. Spring Security `CustomPermissionEvaluator.hasPermission(...)` metodunu çağırır.
4. `CustomPermissionEvaluator`, DB'den ID'si 5 olan `LeaveRequest` kaydını bulur.
5. İstekteki `CustomUserPrincipal.getId()` ile kaydın sahibi karşılaştırılır veya yetki `ADMIN` mi bakılır.
6. Geçerli ise metot çalışır; değilse `403 Forbidden` döner.

---

## 8. Önemli Sınıflar

* **`CustomPermissionEvaluator`**: Nesne türüne (`LeaveRequest`) ve istenen izne (`READ`, `APPROVE` vs.) göre yetkilendirme kararını veren sınıf.
* **`LeaveRequestController`**: Fine-grained yetkilendirme anotasyonlarına sahip denetleyici.

---

## 9. Önemli Teknik Kavramlar

* **IDOR (Insecure Direct Object Reference)**: Bir kullanıcının yetkisi olmayan başka bir nesneye ID değiştirerek erişebilmesi zafiyeti.
* **SpEL (Spring Expression Language)**: Anotasyonlar içinde dinamik Java kodları çalıştırmaya yarayan Spring ifade dili (`#id`, `hasPermission(...)`).
* **ACL (Access Control List)**: Nesne seviyesinde erişim haklarını tanımlayan güvenlik modeli.

---

## 10. Kod Örnekleri

### Custom Permission Evaluator Uygulaması (`CustomPermissionEvaluator.java`)
```java
@Override
public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
    if (authentication == null || targetId == null) return false;

    if ("LeaveRequest".equals(targetType)) {
        Optional<LeaveRequest> leaveRequest = leaveRequestRepository.findById((Long) targetId);
        if (leaveRequest.isEmpty()) return false;
        
        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
        boolean isOwner = leaveRequest.get().getEmployee().getId().equals(principal.getId());
        boolean isAdmin = principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Permission perm = Permission.valueOf(permission.toString().toUpperCase());
        return switch (perm) {
            case READ -> isOwner || isAdmin;
            case WRITE -> isOwner;
            case DELETE -> isAdmin || isOwner;
            case APPROVE -> isAdmin;
        };
    }
    return false;
}
```

---

## 11. API Açıklamaları

| Method | Endpoint | Anotasyon / Erişim | Açıklama |
| :--- | :--- | :--- | :--- |
| **GET** | `/leave-requests/{id}` | `hasPermission(#id, 'LeaveRequest', 'READ')` | Sadece talebin sahibi veya Admin okuyabilir. |
| **POST** | `/leave-requests/{id}/approve` | `hasPermission(#id, 'LeaveRequest', 'APPROVE')` | Sadece Admin onaylayabilir. |
| **DELETE** | `/leave-requests/{id}` | `hasPermission(#id, 'LeaveRequest', 'DELETE')` | Talebin sahibi veya Admin silebilir. |

---

## 12. Kurulum

1. `SecurityConfig` sınıfına `@EnableMethodSecurity` eklendiğinden emin olun.
2. Uygulamayı çalıştırıp iki farklı kullanıcı ile oluşturulan izin taleplerine çapraz erişim denemeleri yapın.

---

## 13. Konfigürasyon

Metot seviyesi güvenlik `SecurityConfig` üzerindeki `@EnableMethodSecurity` ile aktifleştirilmiştir.

---

## 14. Güvenlik

* **IDOR Saldırılarına Karşı Tam Koruma**: Kullanıcılar sadece kendi ürettikleri veriler üzerinde yetkiye sahiptir.

---

## 15. Veri Akışı

```
Request with ID -> SpEL Interceptor -> CustomPermissionEvaluator -> DB Lookup -> Ownership Matching -> Execution / Block
```

---

## 16. Hata Yönetimi

* İzin verilmeyen isteklerde `JwtAccessDeniedHandler` otomatik tetiklenerek `403 Forbidden` yanıtı verir.

---

## 17. Performans

* Her metot çağrısında veritabanından nesne okumak I/O yükü getirebilir. İhtiyaç halinde `LeaveRequest` sahiplik bilgisi önbelleğe (Cache) alınabilir.

---

## 18. Geliştirici Notları

* Yeni bir domain objesi (örneğin `Document`, `Invoice`) eklendiğinde `CustomPermissionEvaluator` içerisine yeni bir `switch/case` kolu eklenmelidir.

---

## 19. Gelecekte Yapılabilecek Geliştirmeler

- [ ] Spring Security ACL (Domain Object Security) veri tabanlı izin tabloları entegrasyonu.
