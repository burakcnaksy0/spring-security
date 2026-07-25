# 🗄️ Dynamic Database Authorities & RBAC/ABAC Architecture (`dynamic_db_authorities` Branch)

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

Bu branch, sabit Enum tabanlı rol yönetiminden çıkarak, veritabanında tamamen **Dinamik Yönetilebilir Rol (Role) ve İzin (Permission / Privilege)** veritabanı ilişkileri sunan kurumsal **RBAC / ABAC Yetkilendirme Mimarisi** sunar.

### Amaç ve Kapsam
Sistem yöneticilerinin (ADMIN) runtime esnasında herhangi bir kod değişikliği veya deployment yapmadan REST API üzerinden yeni roller (`Role`) ve izinler (`Permission`) tanımlayabilmesini, rollere izin bağlayabilmesini ve kullanıcılara bu roller atanarak anlık yetkilendirme yapılabilmesini sağlamaktır.

---

## 2. Problem Tanımı

Sert kodlanmış (Hardcoded) Enum rol yapısının (`Role.USER`, `Role.ADMIN`) kurumsal sistemlerdeki yetersizlikleri:
1. **Dinamizm Eksikliği**: Yeni bir alt yetki seviyesi (örn. `HR_MANAGER`, `FINANCE_VIEWER`) eklendiğinde Java Enum'ının değiştirilip uygulamanın yeniden derlenmesi (recompile) ve deploy edilmesi gerekir.
2. **Granular Permission İhtiyacı**: "ADMIN her şeyi yapar" yaklaşımı yerine "Faturaları okuyabilir ama silemez" şeklinde ince taneli (fine-grained) yetkilerin dinamik oluşturulamaması.

---

## 3. Çözüm Yaklaşımı

* **Veritabanı Nesnesi Olarak `Role` ve `Permission`**:
  * `Role`: JPA Entity (`id`, `name`, `Set<Permission> permissions`). `@ManyToMany` ilişki.
  * `Permission`: JPA Entity (`id`, `name`).
  * `Employee`: `@ManyToMany Set<Role> roles`.
* **Spring Security `GrantedAuthority` Senkronizasyonu**:
  * `CustomUserDetailsService` çalışırken, kullanıcının sahip olduğu tüm `Role` nesnelerini ve bu rollerin içerdiği tüm `Permission` nesnelerini düz bir `GrantedAuthority` listesine dönüştürür.
  * Roller `ROLE_ADMIN`, izinler `LEAVE_APPROVE`, `USER_READ` formatında Spring `SecurityContextHolder`'a yüklenir.
* **Runtime Management APIs**: Yöneticilerin yeni izin/rol oluşturması ve ataması için `/api/v1/admin/**` uç noktaları.

---

## 4. Kullanılan Teknolojiler

| Teknoloji | Kullanım Amacı |
| :--- | :--- |
| **Spring Data JPA Many-To-Many** | `employee_roles` ve `role_permissions` ara tablolarının `@ManyToMany(fetch = FetchType.EAGER)` ile yönetilmesi. |
| **Spring Security GrantedAuthority** | Veritabanındaki yetkilerin Spring Security context'ine dinamik aktarılması. |
| **MySQL / JPA Cascading** | İlişkisel rol ve izin verilerinin bütünlüğünün korunması. |

---

## 5. Proje Mimarisi

```
[ Database Model ]
Employee ◄──(ManyToMany)──► Role ◄──(ManyToMany)──► Permission

[ User Login / Authentication ]
       │
       ▼
CustomUserDetailsService.loadUserByUsername()
       │
       ▼
Extract Employee -> Fetch Roles -> Fetch Permissions for Each Role
       │
       ▼
Map to Collection<GrantedAuthority> (e.g. "ROLE_ADMIN", "LEAVE_READ", "LEAVE_APPROVE")
       │
       ▼
Spring SecurityContextHolder (Available for @PreAuthorize or Filter Checks)
```

---

## 6. Klasör Yapısı

```
src/main/java/com/burakcanaksoy/springsecurity/
├── entity/
│   ├── Role.java                       # Dynamic Role JPA Entity
│   ├── Permission.java                 # Dynamic Permission JPA Entity
│   └── Employee.java                   # Set<Role> roles ilişkisi
├── controller/
│   └── RolePermissionController.java   # Dynamic Role/Permission CRUD ve Atama Uç Noktaları (/api/v1/admin)
├── service/
│   └── RolePermissionService.java      # Dynamic Role & Permission yönetim servisi
└── repository/
    ├── RoleRepository.java
    └── PermissionRepository.java
```

---

## 7. Kodun Genel Akışı

1. Yönetici `/api/v1/admin/permissions` uç noktasından `LEAVE_APPROVE` iznini oluşturur.
2. Yönetici `/api/v1/admin/roles` uç noktasından `HR_MANAGER` rolünü tanımlar.
3. Yönetici `/api/v1/admin/roles/{roleId}/permissions` ile izni role bağlar.
4. Yönetici `/api/v1/admin/employees/{employeeId}/roles` ile rolü bir çalışana atar.
5. Çalışan ilk isteğinde `CustomUserDetailsService` DB'den rollerini ve izinlerini okur. Spring `hasAuthority('LEAVE_APPROVE')` kontrolünü anında doğrular.

---

## 8. Önemli Sınıflar

* **`RolePermissionController`**: Yöneticilerin veritabanındaki rol ve izin matrisini yönetmesini sağlayan REST denetleyicisi.
* **`RolePermissionService`**: Rol oluşturma, izin bağlama ve çalışana rol atama mantıklarını yürüten iş katmanı servisi.
* **`Role` & `Permission`**: Veritabanı tablolarını temsil eden JPA entity sınıfları.

---

## 9. Önemli Teknik Kavramlar

* **RBAC (Role-Based Access Control)**: Kullanıcılara yetkilerin doğrudan değil, sahip oldukları roller üzerinden atanması modeli.
* **ABAC / Fine-Grained Authorization**: İzinlerin en alt parçaya (Permission/Privilege) kadar ayrıştırılarak esnek olarak birleştirilmesi.
* **EAGER Fetching**: `Role` nesneleri yüklenirken izinlerin de anında bellek tutulması (`FetchType.EAGER`).

---

## 10. Kod Örnekleri

### Dinamik Rol & İzin Atama Endpoint'leri (`RolePermissionController.java`)
```java
@PostMapping("/roles/{roleId}/permissions")
public ResponseEntity<Role> assignPermissionsToRole(
        @PathVariable Long roleId,
        @Valid @RequestBody AssignPermissionsRequest request) {
    return ResponseEntity.ok(rolePermissionService.assignPermissionsToRole(roleId, request));
}

@PostMapping("/employees/{employeeId}/roles")
public ResponseEntity<EmployeeResponse> assignRolesToEmployee(
        @PathVariable Long employeeId,
        @Valid @RequestBody AssignRolesRequest request) {
    return ResponseEntity.ok(rolePermissionService.assignRolesToEmployee(employeeId, request));
}
```

---

## 11. API Açıklamaları

| Method | Endpoint | Erişim | Açıklama |
| :--- | :--- | :--- | :--- |
| **POST** | `/api/v1/admin/permissions` | `ROLE_ADMIN` | Yeni dinamik izin oluşturur. |
| **POST** | `/api/v1/admin/roles` | `ROLE_ADMIN` | Yeni dinamik rol oluşturur. |
| **POST** | `/api/v1/admin/roles/{id}/permissions` | `ROLE_ADMIN` | İzinleri bir role bağlar. |
| **POST** | `/api/v1/admin/employees/{id}/roles` | `ROLE_ADMIN` | Rolleri bir çalışana atar. |

---

## 12. Kurulum

1. Veritabanında `role`, `permission`, `role_permissions` ve `employee_roles` tabloları JPA hibernate `ddl-auto=update` ile otomatik oluşturulur.
2. Uygulama başladığında varsayılan `ROLE_ADMIN` ve `ROLE_USER` kayıtlarının veritabanına eklenmesi önerilir.

---

## 13. Konfigürasyon

* `@PreAuthorize("hasAuthority('ROLE_ADMIN') or hasRole('ADMIN')")`

---

## 14. Güvenlik

* Yetki atama uç noktaları (`/api/v1/admin/**`) sadece kök `ROLE_ADMIN` yetkisine sahip kullanıcılar tarafından çağrılabilir.

---

## 15. Veri Akışı

```
Admin Request -> Create Permission -> Create Role -> Link Role-Permission -> Link Employee-Role -> User SecurityContext Updated
```

---

## 16. Hata Yönetimi

* Olmayan bir rol veya izin ID'si gönderildiğinde `ResourceNotFoundException` dönülür.

---

## 17. Performans

* `@ManyToMany(fetch = FetchType.EAGER)` kullanımı çok fazla rol ve izin olduğunda N+1 sorgu problemine yol açabilir. Üretim ortamında `@EntityGraph` veya JOIN FETCH ile optimize edilmelidir.

---

## 18. Geliştirici Notları

* JWT Access Token içerisine de kullanıcının rollerini claim olarak ekleyerek veritabanı sorgu yükü azaltılabilir.

---

## 19. Gelecekte Yapılabilecek Geliştirmeler

- [ ] Redis ile Rol/İzin önbellekleme (Caching).
- [ ] Hiyerarşik Rol Yapısı (`RoleHierarchy` Bean entegrasyonu).
