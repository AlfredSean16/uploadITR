package com.metrobank.uploadITR.controller;

import com.metrobank.uploadITR.dto.*;
import com.metrobank.uploadITR.entity.ItrRecord;
import com.metrobank.uploadITR.exception.BusinessException;
import com.metrobank.uploadITR.exception.FileProcessingException;
import com.metrobank.uploadITR.service.BlobService;
import com.metrobank.uploadITR.service.ItrService;
import com.metrobank.uploadITR.service.MailService;
import com.metrobank.uploadITR.util.PdfPasswordUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/admin/itr")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200", "http://localhost:8080", "http://localhost:8081"})
@PreAuthorize("hasRole('ADMIN')")
public class ItrController {
    private static final Logger logger = LoggerFactory.getLogger(ItrController.class);
    private static final String OWNER_PASSWORD = "MetroBankOwnerKey2024!";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private final ItrService itrService;
    private final BlobService blobService;
    private final MailService mailService;

    public ItrController(ItrService itrService, BlobService blobService, MailService mailService) {
        this.itrService = itrService;
        this.blobService = blobService;
        this.mailService = mailService;
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<ItrResponse>> uploadItr(
            @Valid @ModelAttribute ItrUploadRequest request,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpRequest) {

        try {
            logger.info("ITR upload request for user {} and year {}", request.getUserId(), request.getYear());

            validateFileUpload(file);

            // Generate timestamped filename
            String timestampedFilename = generateTimestampedFilename(request.getFilename());

            // Create temporary file for processing
            Path tempFile = Files.createTempFile("itr_upload_", ".pdf");
            file.transferTo(tempFile.toFile());

            // Generate password and encrypt PDF
            String pdfPassword = PdfPasswordUtil.generatePassword();
            PdfPasswordUtil.encryptPdf(tempFile.toFile(), OWNER_PASSWORD, pdfPassword);

            // Upload to blob storage and get the blob URL
            String blobUrl;
            try (InputStream inputStream = Files.newInputStream(tempFile)) {
                blobUrl = blobService.uploadFile(timestampedFilename, inputStream, tempFile.toFile().length());
            }

            // Clean up temp file
            Files.deleteIfExists(tempFile);

            // Create ITR record in database with the blob URL
            ItrRecord itrRecord = itrService.createItrRecord(
                    request.getUserId(),
                    request.getYear(),
                    blobUrl,  // Store the full Azure Blob Storage URL
                    timestampedFilename,
                    pdfPassword
            );

            // Send password email
            sendPasswordEmail(request.getUserId(), request.getYear(), timestampedFilename, pdfPassword);

            // Send upload confirmation
            sendUploadNotification(request.getUserId(), request.getYear(), timestampedFilename);

            ItrResponse response = convertToResponse(itrRecord);

            logger.info("ITR upload completed successfully for user {} and year {}", request.getUserId(), request.getYear());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("ITR uploaded successfully and password sent via email", response));

        } catch (Exception e) {
            logger.error("ITR upload failed for user {} and year {}: {}", request.getUserId(), request.getYear(), e.getMessage(), e);
            throw new FileProcessingException("ITR upload failed", e);
        }
    }

    @PutMapping("/update")
    public ResponseEntity<ApiResponse<ItrResponse>> updateItr(
            @Valid @ModelAttribute ItrUpdateRequest request,
            @RequestParam(value = "file", required = false) MultipartFile file) {

        try {
            logger.info("ITR update request for ITR ID {} and year {}", request.getItrId(), request.getYear());

            // Get existing ITR record
            ItrResponse existingItr = itrService.getItrRecordById(request.getItrId());
            boolean yearChanged = !existingItr.getYear().equals(request.getYear());
            boolean fileProvided = file != null && !file.isEmpty();

            String filename = existingItr.getFilename();
            String pdfPassword = null; // Will be set based on conditions
            String blobUrl = null; // Will store the Azure Blob URL

            if (fileProvided) {
                validateFileUpload(file);
                filename = generateTimestampedFilename(request.getFilename());

                // Process new file
                Path tempFile = Files.createTempFile("itr_update_", ".pdf");
                file.transferTo(tempFile.toFile());

                // Generate new password if year changed, otherwise keep existing
                if (yearChanged) {
                    pdfPassword = PdfPasswordUtil.generatePassword();
                    PdfPasswordUtil.encryptPdf(tempFile.toFile(), OWNER_PASSWORD, pdfPassword);
                } else {
                    // For same year, we'd need to get the existing password
                    // This is a security consideration - for now, generate new password
                    pdfPassword = PdfPasswordUtil.generatePassword();
                    PdfPasswordUtil.encryptPdf(tempFile.toFile(), OWNER_PASSWORD, pdfPassword);
                }

                // Upload new file and get blob URL
                try (InputStream inputStream = Files.newInputStream(tempFile)) {
                    blobUrl = blobService.uploadFile(filename, inputStream, tempFile.toFile().length());
                }

                // Delete old file
                blobService.deleteFile(existingItr.getFilename());

                Files.deleteIfExists(tempFile);

            } else if (yearChanged) {
                // Year changed but no new file - re-encrypt existing file with new password
                filename = generateTimestampedFilename(request.getFilename());
                pdfPassword = PdfPasswordUtil.generatePassword();

                Path tempFile = Files.createTempFile("itr_reencrypt_", ".pdf");
                try (InputStream oldFileStream = blobService.downloadFile(existingItr.getFilename())) {
                    Files.copy(oldFileStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
                }

                // This would need the old password - for security, we'll generate new
                PdfPasswordUtil.encryptPdf(tempFile.toFile(), OWNER_PASSWORD, pdfPassword);

                try (InputStream inputStream = Files.newInputStream(tempFile)) {
                    blobUrl = blobService.uploadFile(filename, inputStream, tempFile.toFile().length());
                }

                blobService.deleteFile(existingItr.getFilename());
                Files.deleteIfExists(tempFile);
            } else {
                // No file change and no year change - keep existing password
                // We don't have access to the existing password, so we'll generate new
                pdfPassword = PdfPasswordUtil.generatePassword();
                filename = generateTimestampedFilename(request.getFilename());

                // Re-encrypt existing file with new password
                Path tempFile = Files.createTempFile("itr_reencrypt_", ".pdf");
                try (InputStream oldFileStream = blobService.downloadFile(existingItr.getFilename())) {
                    Files.copy(oldFileStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
                }

                PdfPasswordUtil.encryptPdf(tempFile.toFile(), OWNER_PASSWORD, pdfPassword);

                try (InputStream inputStream = Files.newInputStream(tempFile)) {
                    blobUrl = blobService.uploadFile(filename, inputStream, tempFile.toFile().length());
                }

                blobService.deleteFile(existingItr.getFilename());
                Files.deleteIfExists(tempFile);
            }

            // Update ITR record in database with the blob URL
            ItrRecord updatedRecord = itrService.updateItrRecord(
                    request.getItrId(),
                    request.getYear(),
                    blobUrl,  // Store the full Azure Blob Storage URL
                    filename,
                    pdfPassword
            );

            // Send notifications
            if (yearChanged || fileProvided) {
                sendPasswordEmail(existingItr.getUserId(), request.getYear(), filename, pdfPassword);
            }
            sendUpdateNotification(existingItr.getUserId(), request.getYear(), filename, yearChanged || fileProvided);

            ItrResponse response = convertToResponse(updatedRecord);

            logger.info("ITR update completed successfully for ITR ID {}", request.getItrId());

            return ResponseEntity.ok(
                    ApiResponse.success("ITR updated successfully", response));

        } catch (Exception e) {
            logger.error("ITR update failed for ITR ID {}: {}", request.getItrId(), e.getMessage(), e);
            throw new FileProcessingException("ITR update failed", e);
        }
    }

    @DeleteMapping("/{itrId}")
    public ResponseEntity<ApiResponse<Void>> removeItr(@PathVariable Long itrId) {
        logger.info("ITR removal request for ITR ID {}", itrId);

        boolean removed = itrService.removeItrRecord(itrId);

        if (removed) {
            logger.info("ITR removed successfully for ITR ID {}", itrId);
            return ResponseEntity.ok(ApiResponse.success("ITR record removed successfully", null));
        } else {
            throw new BusinessException("Failed to remove ITR record with ID: " + itrId);
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ItrResponse>>> getAllItrs() {
        List<ItrResponse> itrs = itrService.getAllActiveItrRecords();
        return ResponseEntity.ok(ApiResponse.success("ITR records retrieved successfully", itrs));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<ItrResponse>>> getItrsByUserId(@PathVariable Long userId) {
        List<ItrResponse> itrs = itrService.getItrRecordsByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("User ITR records retrieved successfully", itrs));
    }

    @GetMapping("/{itrId}")
    public ResponseEntity<ApiResponse<ItrResponse>> getItrById(@PathVariable Long itrId) {
        ItrResponse itr = itrService.getItrRecordById(itrId);
        return ResponseEntity.ok(ApiResponse.success("ITR record retrieved successfully", itr));
    }

    // Helper methods
    private void validateFileUpload(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("File is required");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("File size exceeds maximum limit of 10MB");
        }

        if (!"application/pdf".equals(file.getContentType())) {
            throw new BusinessException("Only PDF files are allowed");
        }
    }

    private String generateTimestampedFilename(String originalFilename) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return String.format("%s_%s.pdf", originalFilename.replace(".pdf", ""), timestamp);
    }

    private void sendPasswordEmail(Long userId, Integer year, String filename, String password) {
        try {
            String userEmail = itrService.getUserEmail(userId);
            String userName = itrService.getUserName(userId);
            mailService.sendPasswordEmail(userEmail, userName, year, filename, password);
        } catch (Exception e) {
            logger.warn("Failed to send password email for user {}: {}", userId, e.getMessage());
        }
    }

    private void sendUploadNotification(Long userId, Integer year, String filename) {
        try {
            String userEmail = itrService.getUserEmail(userId);
            String userName = itrService.getUserName(userId);
            mailService.sendItrUploadNotification(userEmail, userName, year, filename);
        } catch (Exception e) {
            logger.warn("Failed to send upload notification for user {}: {}", userId, e.getMessage());
        }
    }

    private void sendUpdateNotification(Long userId, Integer year, String filename, boolean passwordChanged) {
        try {
            String userEmail = itrService.getUserEmail(userId);
            String userName = itrService.getUserName(userId);
            mailService.sendItrUpdateNotification(userEmail, userName, year, filename, passwordChanged);
        } catch (Exception e) {
            logger.warn("Failed to send update notification for user {}: {}", userId, e.getMessage());
        }
    }

    private ItrResponse convertToResponse(ItrRecord record) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return ItrResponse.builder()
                .itrId(record.getItrId())
                .userId(record.getUserId())
                .year(record.getYear())
                .filename(record.getFilename())
                .status(record.getStatus())
                .createdAt(record.getCreatedAt() != null ? record.getCreatedAt().format(formatter) : null)
                .updatedAt(record.getUpdatedAt() != null ? record.getUpdatedAt().format(formatter) : null)
                .hasPassword(record.getPdfPassword() != null && !record.getPdfPassword().isEmpty())
                .build();
    }
}