package com.burakcanaksoy.springsecurity.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "employee")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String passwordHash;
    private String firstName;
    private String lastName;
    private String tcNo;
    private LocalDate birthDate;
    private String gender;
    private String phoneNumber;
    private String email;
    private String address;
    private boolean enabled;
    @Enumerated(EnumType.STRING)
    private Role role;
    /*
    Google Authenticator veya Microsoft Authenticator gibi uygulamaların her 30 saniyede bir benzersiz,
    tek kullanımlık doğrulama kodları (OTP) üretmek için kullandığı gizli metin veya karekod (QR) tabanlı anahtardır.
     */
    @Column
    private String totpSecret;
    @Column(nullable = false)
    private boolean mfaEnabled = false;

}
