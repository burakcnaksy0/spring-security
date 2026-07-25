package com.burakcanaksoy.springsecurity.dto.response;

import com.burakcanaksoy.springsecurity.entity.enums.LeaveRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class LeaveRequestResponse {
    private Long id;
    private Long employeeId;
    private String employeeUsername;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private LeaveRequestStatus status;
    private Instant createdAt;
}
