package com.burakcanaksoy.springsecurity.service;

import com.burakcanaksoy.springsecurity.dto.request.LeaveRequestCreateRequest;
import com.burakcanaksoy.springsecurity.dto.response.LeaveRequestResponse;
import com.burakcanaksoy.springsecurity.entity.Employee;
import com.burakcanaksoy.springsecurity.entity.LeaveRequest;
import com.burakcanaksoy.springsecurity.entity.enums.LeaveRequestStatus;
import com.burakcanaksoy.springsecurity.exception.ResourceNotFoundException;
import com.burakcanaksoy.springsecurity.repository.EmployeeRepository;
import com.burakcanaksoy.springsecurity.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveRequestService {
    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;


    public void delete(Long id) {
        LeaveRequest leaveRequest = getLeaveRequestOrThrow(id);
        leaveRequestRepository.delete(leaveRequest);
    }


    public List<LeaveRequestResponse> findAll() {
        return leaveRequestRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public LeaveRequestResponse reject(Long id) {
        LeaveRequest leaveRequest = getLeaveRequestOrThrow(id);
        if (leaveRequest.getStatus() != LeaveRequestStatus.PENDING) {
            throw new IllegalStateException("Only pending leave requests can be rejected.");
        }
        leaveRequest.setStatus(LeaveRequestStatus.REJECTED);
        LeaveRequest updated = leaveRequestRepository.save(leaveRequest);
        return toResponse(updated);
    }

    public List<LeaveRequestResponse> findMyLeaveRequests() {
        Employee currentEmployee = getCurrentEmployee();
        return leaveRequestRepository.findByEmployeeId(currentEmployee.getId())
                .stream()
                .map(this::toResponse)
                .toList();

    }

    public LeaveRequestResponse create(LeaveRequestCreateRequest request) {
        Employee employee = getCurrentEmployee();
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("End date cannot be before start date.");
        }
        LeaveRequest leaveRequest = LeaveRequest.builder()
                .employee(employee)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .reason(request.getReason())
                .status(LeaveRequestStatus.PENDING)
                .build();
        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
        return toResponse(saved);
    }

    public LeaveRequestResponse approve(Long id) {
        LeaveRequest leaveRequest = getLeaveRequestOrThrow(id);
        if (leaveRequest.getStatus() != LeaveRequestStatus.PENDING) {
            throw new IllegalStateException("Only pending leave requests can be approved.");
        }
        leaveRequest.setStatus(LeaveRequestStatus.APPROVED);
        LeaveRequest updated = leaveRequestRepository.save(leaveRequest);
        return toResponse(updated);
    }

    public LeaveRequestResponse findById(Long id) {
        LeaveRequest leaveRequest = getLeaveRequestOrThrow(id);
        return toResponse(leaveRequest);
    }


    // helper methods
    private LeaveRequest getLeaveRequestOrThrow(Long id) {
        return leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with id: " + id));
    }

    private Employee getCurrentEmployee() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
        return employeeRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
    }

    private LeaveRequestResponse toResponse(LeaveRequest leaveRequest) {
        return LeaveRequestResponse.builder()
                .id(leaveRequest.getId())
                .employeeId(leaveRequest.getEmployee().getId())
                .employeeUsername(leaveRequest.getEmployee().getUsername())
                .startDate(leaveRequest.getStartDate())
                .endDate(leaveRequest.getEndDate())
                .reason(leaveRequest.getReason())
                .status(leaveRequest.getStatus())
                .createdAt(leaveRequest.getCreatedAt())
                .build();
    }
}
