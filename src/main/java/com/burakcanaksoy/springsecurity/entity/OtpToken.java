package com.burakcanaksoy.springsecurity.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "otp_token")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
// (one-time password) sistemlere güvenli giriş yapmak ve işlemleri onaylamak için kullanılan tek seferlik şifredir.
public class OtpToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false, length = 6)
    private String otpCode;

    @Column(nullable = false)
    private Instant expiryDate;

    @Column(nullable = false)
    private boolean used;
}
