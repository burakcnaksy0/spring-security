package com.burakcanaksoy.springsecurity.repository;

import com.burakcanaksoy.springsecurity.entity.Employee;
import com.burakcanaksoy.springsecurity.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    void deleteByEmployee(Employee employee);
    void deleteByEmployeeId(Long employeeId);

    void deleteByToken(String token);
}
