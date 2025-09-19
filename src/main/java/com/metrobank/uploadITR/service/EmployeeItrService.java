package com.metrobank.uploadITR.service;

import com.metrobank.uploadITR.dto.EmployeeItrResponse;
import com.metrobank.uploadITR.dto.EmployeeItrSummary;
import com.metrobank.uploadITR.entity.ItrRecord;
import com.metrobank.uploadITR.exception.BusinessException;
import com.metrobank.uploadITR.exception.ResourceNotFoundException;
import com.metrobank.uploadITR.repository.ItrRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class EmployeeItrService {
    private static final Logger logger = LoggerFactory.getLogger(EmployeeItrService.class);

    private final ItrRepository itrRepository;
    private final BlobService blobService;

    public EmployeeItrService(ItrRepository itrRepository, BlobService blobService) {
        this.itrRepository = itrRepository;
        this.blobService = blobService;
    }

    public EmployeeItrSummary getEmployeeItrRecords(Long userId) {
        // Validate user exists
        if (itrRepository.countUserById(userId) == 0) {
            throw new BusinessException("User with ID " + userId + " does not exist");
        }

        // Get user details
        String userName = itrRepository.findUserNameById(userId)
                .orElse("Unknown User");
        String userEmail = itrRepository.findUserEmailById(userId)
                .orElse("");

        // Get user's ITR records
        List<ItrRecord> itrRecords = itrRepository.findActiveByUserId(userId);

        List<EmployeeItrResponse> employeeItrs = itrRecords.stream()
                .map(this::convertToEmployeeResponse)
                .collect(Collectors.toList());

        List<Integer> availableYears = itrRecords.stream()
                .map(ItrRecord::getYear)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        logger.info("Retrieved {} ITR records for user {}", employeeItrs.size(), userId);

        return EmployeeItrSummary.builder()
                .employeeName(userName)
                .employeeEmail(userEmail)
                .totalItrRecords(employeeItrs.size())
                .availableYears(availableYears)
                .itrRecords(employeeItrs)
                .build();
    }

    public List<EmployeeItrResponse> getEmployeeItrsByYear(Long userId, Integer year) {
        // Validate user exists
        if (itrRepository.countUserById(userId) == 0) {
            throw new BusinessException("User with ID " + userId + " does not exist");
        }

        List<ItrRecord> itrRecords = itrRepository.findActiveByUserId(userId).stream()
                .filter(itr -> itr.getYear().equals(year))
                .collect(Collectors.toList());

        logger.info("Retrieved {} ITR records for user {} and year {}", itrRecords.size(), userId, year);

        return itrRecords.stream()
                .map(this::convertToEmployeeResponse)
                .collect(Collectors.toList());
    }

    public ResponseEntity<Resource> downloadItrFile(Long userId, Long itrId, String providedPassword) {
        // Get ITR record and validate ownership
        ItrRecord itrRecord = itrRepository.findActiveById(itrId)
                .orElseThrow(() -> new ResourceNotFoundException("ITR record not found: " + itrId));

        // Ensure the ITR belongs to the requesting user
        if (!itrRecord.getUserId().equals(userId)) {
            throw new BusinessException("Access denied. You can only download your own ITR files.");
        }

        // Validate password if provided (optional - could be used for additional security)
        if (providedPassword != null && !providedPassword.isEmpty()) {
            // Note: In a real implementation, you might want to verify the password
            // against the stored PDF password, but since PDFs are already encrypted,
            // this is mainly for additional security layer
            if (!providedPassword.equals(itrRecord.getPdfPassword())) {
                throw new BusinessException("Invalid password provided");
            }
        }

        try {
            // Download file from blob storage
            InputStream fileStream = blobService.downloadFile(itrRecord.getFilename());
            InputStreamResource resource = new InputStreamResource(fileStream);

            // Prepare response headers
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + itrRecord.getFilename() + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE);

            logger.info("User {} downloading ITR file: {}", userId, itrRecord.getFilename());

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);

        } catch (Exception e) {
            logger.error("Failed to download ITR file for user {} and ITR {}: {}", userId, itrId, e.getMessage(), e);
            throw new BusinessException("Failed to download ITR file: " + e.getMessage());
        }
    }

    public boolean hasAccessToItr(Long userId, Long itrId) {
        return itrRepository.findActiveById(itrId)
                .map(itr -> itr.getUserId().equals(userId))
                .orElse(false);
    }

    private EmployeeItrResponse convertToEmployeeResponse(ItrRecord record) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");

        return EmployeeItrResponse.builder()
                .itrId(record.getItrId())
                .year(record.getYear())
                .filename(record.getFilename())
                .status(record.getStatus())
                .uploadedDate(record.getCreatedAt() != null ?
                        record.getCreatedAt().format(formatter) : "Unknown")
                .hasPassword(record.getPdfPassword() != null && !record.getPdfPassword().isEmpty())
                .canDownload(record.isActive() && blobService.exists(record.getFilename()))
                .build();
    }
}