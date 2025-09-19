package com.metrobank.uploadITR.controller;

import com.metrobank.uploadITR.dto.*;
import com.metrobank.uploadITR.service.EmployeeItrService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employee/itr")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200", "http://localhost:8080", "http://localhost:8081"})
@PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
public class EmployeeItrController {
    private static final Logger logger = LoggerFactory.getLogger(EmployeeItrController.class);

    private final EmployeeItrService employeeItrService;

    public EmployeeItrController(EmployeeItrService employeeItrService) {
        this.employeeItrService = employeeItrService;
    }

    @GetMapping("/my-records")
    public ResponseEntity<ApiResponse<EmployeeItrSummary>> getMyItrRecords(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);

        logger.info("Employee {} requesting their ITR records", userId);

        EmployeeItrSummary summary = employeeItrService.getEmployeeItrRecords(userId);

        return ResponseEntity.ok(
                ApiResponse.success("ITR records retrieved successfully", summary));
    }

    @GetMapping("/my-records/{year}")
    public ResponseEntity<ApiResponse<List<EmployeeItrResponse>>> getMyItrRecordsByYear(
            @PathVariable Integer year, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);

        logger.info("Employee {} requesting ITR records for year {}", userId, year);

        List<EmployeeItrResponse> itrs = employeeItrService.getEmployeeItrsByYear(userId, year);

        return ResponseEntity.ok(
                ApiResponse.success("ITR records for year " + year + " retrieved successfully", itrs));
    }

    @GetMapping("/download/{itrId}")
    public ResponseEntity<Resource> downloadMyItr(
            @PathVariable Long itrId,
            @RequestParam(value = "password", required = false) String password,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);

        logger.info("Employee {} requesting download of ITR {}", userId, itrId);

        return employeeItrService.downloadItrFile(userId, itrId, password);
    }

    @PostMapping("/download")
    public ResponseEntity<Resource> downloadMyItrWithPassword(
            @RequestBody ItrDownloadRequest downloadRequest,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);

        logger.info("Employee {} requesting password-protected download of ITR {}", userId, downloadRequest.getItrId());

        return employeeItrService.downloadItrFile(userId, downloadRequest.getItrId(), downloadRequest.getPassword());
    }

    @GetMapping("/check-access/{itrId}")
    public ResponseEntity<ApiResponse<Boolean>> checkItrAccess(
            @PathVariable Long itrId, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);

        boolean hasAccess = employeeItrService.hasAccessToItr(userId, itrId);

        return ResponseEntity.ok(
                ApiResponse.success("Access check completed", hasAccess));
    }

    @GetMapping("/my-profile")
    public ResponseEntity<ApiResponse<EmployeeProfile>> getMyProfile(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        EmployeeProfile profile = EmployeeProfile.builder()
                .userId(userId)
                .username(auth.getName())
                .role(auth.getAuthorities().toString())
                .build();

        return ResponseEntity.ok(
                ApiResponse.success("Profile retrieved successfully", profile));
    }

    // Helper method to get current user ID from JWT token
    private Long getCurrentUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new RuntimeException("User ID not found in request. Authentication may be invalid.");
        }
        return userId;
    }
}

// Additional DTO for employee profile
@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Builder
class EmployeeProfile {
    private Long userId;
    private String username;
    private String role;
}
