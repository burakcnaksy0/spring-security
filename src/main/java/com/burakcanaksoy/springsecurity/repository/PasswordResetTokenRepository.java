package com.burakcanaksoy.springsecurity.repository;

import com.burakcanaksoy.springsecurity.entity.PasswordResetToken;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;


public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    //Optional<PasswordResetToken> findByToken(String token);

    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetToken p WHERE p.employee.id = :employeeId")
    void deleteByEmployeeId(@Param("employeeId") Long employeeId);

    Optional<PasswordResetToken> findByHashedToken(String hashedToken);
}
