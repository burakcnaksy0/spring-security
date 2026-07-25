package com.burakcanaksoy.springsecurity.controller;

import com.burakcanaksoy.springsecurity.dto.request.LeaveRequestCreateRequest;
import com.burakcanaksoy.springsecurity.dto.response.LeaveRequestResponse;
import com.burakcanaksoy.springsecurity.service.LeaveRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/leave-requests")
@RequiredArgsConstructor
public class LeaveRequestController {
    private final LeaveRequestService leaveRequestService;

    @PostMapping
    public ResponseEntity<LeaveRequestResponse> create(@Valid @RequestBody LeaveRequestCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leaveRequestService.create(request));
    }

    @PreAuthorize("hasPermission(#id, 'LeaveRequest', 'READ')")
    @GetMapping("/{id}")
    public ResponseEntity<LeaveRequestResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(leaveRequestService.findById(id));
    }

    @GetMapping("/my")
    public ResponseEntity<List<LeaveRequestResponse>> getMyLeaveRequests() {
        return ResponseEntity.ok(leaveRequestService.findMyLeaveRequests());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<LeaveRequestResponse>> getAll() {
        return ResponseEntity.ok(leaveRequestService.findAll());
    }

    @PreAuthorize("hasPermission(#id, 'LeaveRequest', 'APPROVE')")
    @PostMapping("/{id}/approve")
    public ResponseEntity<LeaveRequestResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(leaveRequestService.approve(id));
    }

    @PreAuthorize("hasPermission(#id, 'LeaveRequest', 'APPROVE')")
    @PostMapping("/{id}/reject")
    public ResponseEntity<LeaveRequestResponse> reject(@PathVariable Long id) {
        return ResponseEntity.ok(leaveRequestService.reject(id));
    }

    @PreAuthorize("hasPermission(#id, 'LeaveRequest', 'DELETE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        leaveRequestService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
