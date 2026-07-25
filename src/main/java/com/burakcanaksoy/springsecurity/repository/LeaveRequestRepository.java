package com.burakcanaksoy.springsecurity.repository;

import com.burakcanaksoy.springsecurity.entity.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByEmployeeId(Long employeeId);
}
