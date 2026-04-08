package com.krushikranti.controller;

import com.krushikranti.dto.response.ApiResponse;
import com.krushikranti.exception.ResourceNotFoundException;
import com.krushikranti.model.FraudAlert;
import com.krushikranti.repository.FraudAlertRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fraud-alerts")
@RequiredArgsConstructor
@Tag(name = "Fraud Alerts", description = "Fraud alert management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class FraudAlertController {

    private static final String STATUS_RESOLVED = "RESOLVED";
    private static final String STATUS_DISMISSED = "DISMISSED";

    private final FraudAlertRepository fraudAlertRepository;

    @GetMapping
    @Operation(summary = "Get all fraud alerts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<FraudAlert>>> getAllAlerts() {
        List<FraudAlert> alerts = fraudAlertRepository.findAllByOrderByCreatedAtDesc();
        return ResponseEntity.ok(ApiResponse.success("Fraud alerts fetched successfully", alerts));
    }

    @PostMapping("/{id}/resolve")
    @Operation(summary = "Mark fraud alert as RESOLVED")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FraudAlert>> resolveAlert(@PathVariable Long id) {
        FraudAlert updated = updateStatus(id, STATUS_RESOLVED);
        return ResponseEntity.ok(ApiResponse.success("Fraud alert resolved successfully", updated));
    }

    @PostMapping("/{id}/dismiss")
    @Operation(summary = "Mark fraud alert as DISMISSED")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FraudAlert>> dismissAlert(@PathVariable Long id) {
        FraudAlert updated = updateStatus(id, STATUS_DISMISSED);
        return ResponseEntity.ok(ApiResponse.success("Fraud alert dismissed successfully", updated));
    }

    private FraudAlert updateStatus(Long id, String status) {
        FraudAlert alert = fraudAlertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FraudAlert", "id", id));
        alert.setStatus(status);
        return fraudAlertRepository.save(alert);
    }
}
