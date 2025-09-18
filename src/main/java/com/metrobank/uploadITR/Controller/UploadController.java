package com.metrobank.uploadITR.Controller;

import com.metrobank.uploadITR.DTO.UploadDTO;
import com.metrobank.uploadITR.MailService.MailService;
import com.metrobank.uploadITR.blobs.BlobService;
import com.metrobank.uploadITR.exception.ItrIdValidationException;
import com.metrobank.uploadITR.exception.UserIdValidationException;
import com.metrobank.uploadITR.model.UploadModel;
import com.metrobank.uploadITR.repository.UploadRepository;
import com.metrobank.uploadITR.service.Upload;
import com.metrobank.uploadITR.PdfPassword.PdfPasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping(value = "/HRHome")
public class UploadController {
    private final Upload upload;

    @Autowired
    private UploadRepository uploadRepository;

    @Autowired
    private MailService emailService;

    private BlobService blobService;

    @Autowired
    public UploadController(Upload upload, BlobService blobService){
        this.upload = upload;
        this.blobService = blobService;
    }
    @PostMapping("/indexHR")
    public String index() {
        return "indexHR.html";
    }



    @PostMapping("/upload")
    public ResponseEntity<?> uploadSample(
            @RequestParam(value = "user_id", required = false) String user_id,
            @RequestParam(value = "year", required = false) String year,
            @RequestParam("filename") String filename,
            @RequestParam("uploadFile") MultipartFile uploadFile) {

        try {
            if (user_id == null || user_id.trim().isEmpty() ||
                    year == null || year.trim().isEmpty() ||
                    filename == null || filename.trim().isEmpty() ||
                    uploadFile == null || uploadFile.isEmpty()) {
                throw new ItrIdValidationException("All fields must be filled.");
            }

            int parsedUserId = Integer.parseInt(user_id);
            int parsedYear = Integer.parseInt(year);

            int currentYear = LocalDateTime.now(ZoneId.of("UTC")).getYear();
            if (parsedYear > currentYear) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("You cannot upload ITRs for future years.");
            }

            if (uploadRepository.existByUserId(parsedUserId) == 0) {
                throw new UserIdValidationException("This user does not exist.");
            }

            if (uploadRepository.existsByUserIdAndYear(parsedUserId, parsedYear) > 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("This user already had an ITR for year " + parsedYear + ". Please use update.");
            }

            if (!uploadFile.getContentType().equals("application/pdf")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Invalid file type. Only PDF files are allowed.");
            }

            LocalDateTime dateTime = LocalDateTime.now(ZoneId.of("UTC"));
            String timestampedFilename = String.format("%s_%s.pdf", filename,
                    dateTime.format(DateTimeFormatter.ofPattern("ddMMyyyy_HHmmss")));

            Path tempFile = Files.createTempFile("itr_", ".pdf");
            uploadFile.transferTo(tempFile.toFile());

            String pdfPassword = PdfPasswordUtil.generatePassword();
            PdfPasswordUtil.encryptPdf(tempFile.toFile(), "OwnerSecretKey123", pdfPassword);

            try (InputStream inputStream = Files.newInputStream(tempFile)) {
                blobService.uploadFile(timestampedFilename, inputStream, tempFile.toFile().length());
            }
            Files.deleteIfExists(tempFile);

            if (upload.upload(parsedUserId, parsedYear, "AzureBlob", timestampedFilename, pdfPassword)) {
                String userEmail = uploadRepository.findEmailByUserId(parsedUserId);
                if (userEmail != null && !userEmail.isEmpty()) {
                    emailService.sendPasswordEmail(userEmail, String.valueOf(parsedYear), timestampedFilename, pdfPassword);
                }
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(String.format("File %s uploaded successfully. PDF password sent to %s", timestampedFilename, userEmail));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to save record.");
            }

        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("User ID and Year must be valid numbers.");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("File processing failed: " + e.getMessage());
        }
    }

    @PostMapping(value = "/update")
    public ResponseEntity<?> updateSample(
            @RequestParam(value = "itr_id", required = false) String itr_id,
            @RequestParam(value = "year", required = false) String year,
            @RequestParam("filename") String filename) {

        if (itr_id == null || itr_id.trim().isEmpty() ||
                year == null || year.trim().isEmpty() ||
                filename == null || filename.trim().isEmpty()) {
            throw new ItrIdValidationException("All fields must be filled.");
        }

        int parsedItrId, parsedYear;
        try {
            parsedItrId = Integer.parseInt(itr_id.trim());
            parsedYear = Integer.parseInt(year.trim());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("ITR ID and Year must be valid numbers.");
        }

        int currentYear = LocalDateTime.now(ZoneId.of("UTC")).getYear();
        if (parsedYear > currentYear) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("You cannot upload ITRs for future years.");
        }

        try {
            UploadModel oldRecord = uploadRepository.findById((long) parsedItrId).orElse(null);

            if (oldRecord == null || !"active".equalsIgnoreCase(oldRecord.getStatus())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("This ITR record does not exist or is already removed.");
            }

            int duplicates = uploadRepository.countByUserIdAndYearExcludingItr(
                    oldRecord.getUserId(), parsedYear, parsedItrId);
            if (duplicates > 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("This user already has an ITR for year " + parsedYear + ". Update rejected.");
            }

            boolean yearChanged = parsedYear != oldRecord.getYear();

            String updatedFilename = String.format("%s_%s.pdf", filename,
                    LocalDateTime.now(ZoneId.of("UTC"))
                            .format(DateTimeFormatter.ofPattern("ddMMyyyy_HHmmss")));

            String pdfPassword = oldRecord.getPdfPassword();

            Path tempFile = Files.createTempFile("itr_update_", ".pdf");
            try (InputStream oldBlobStream = blobService.downloadFile(oldRecord.getFilename())) {
                Files.copy(oldBlobStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }

            if (yearChanged) {
                pdfPassword = PdfPasswordUtil.generatePassword();
                PdfPasswordUtil.reEncryptPdf(tempFile.toFile(),
                        oldRecord.getPdfPassword(),
                        tempFile.toFile(),
                        "OwnerSecretKey123",
                        pdfPassword);
            }

            try (InputStream inputStream = Files.newInputStream(tempFile)) {
                blobService.uploadFile(updatedFilename, inputStream, tempFile.toFile().length());
            }

            blobService.deleteFile(oldRecord.getFilename());
            Files.deleteIfExists(tempFile);

            boolean updated = upload.update(parsedItrId, parsedYear, "AzureBlob", updatedFilename, pdfPassword);
            if (!updated) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Failed to update ITR record with ID: " + parsedItrId);
            }

            if (yearChanged) {
                String userEmail = uploadRepository.findEmailByUserId(oldRecord.getUserId());
                if (userEmail != null && !userEmail.isEmpty()) {
                    emailService.sendPasswordEmail(userEmail, String.valueOf(parsedYear), updatedFilename, pdfPassword);
                }
            }

            return ResponseEntity.ok("ITR record " + parsedItrId + " successfully updated.");

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("File update failed: " + e.getMessage());
        }
    }


    @PostMapping("/remove")
    public ResponseEntity<?> removeSample(@RequestParam(value = "itr_id", required = false) String itr_id) {
        if (itr_id == null || itr_id.trim().isEmpty()) {
            throw new ItrIdValidationException("Please fill up the field.");
        }

        int parsedItrID;
        try {
            parsedItrID = Integer.parseInt(itr_id.trim());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("ITR ID must be a valid number.");
        }

        boolean removed = upload.remove(parsedItrID);
        if (removed) {
            return ResponseEntity.ok(String.format("ITR number: %s is successfully marked as removed", parsedItrID));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("ITR record not found or already removed.");
        }
    }
}


