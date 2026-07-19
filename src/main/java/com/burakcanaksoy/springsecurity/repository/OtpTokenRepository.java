package com.burakcanaksoy.springsecurity.repository;

import com.burakcanaksoy.springsecurity.entity.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    @Query(value = "SELECT * FROM otp_token WHERE employee_id = :employeeId AND otp_code = :otpCode AND used = false", nativeQuery = true)
    Optional<OtpToken> findValidOtp(@Param("employeeId") Long employeeId, @Param("otpCode") String otpCode);


    @Modifying
    @Query("DELETE FROM OtpToken o WHERE o.employee.id = :employeeId")
    void deleteByEmployeeId(@Param("employeeId") Long employeeId);

}

