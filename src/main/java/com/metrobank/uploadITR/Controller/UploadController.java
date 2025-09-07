package com.metrobank.uploadITR.Controller;

import com.metrobank.uploadITR.DTO.UploadDTO;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping(value = "/HRHome")
public class UploadController {
    private final Upload upload;

    @Autowired
    private UploadRepository uploadRepository;

    @Autowired
    public UploadController(Upload upload){
        this.upload = upload;
    }
    @PostMapping("/indexHR")
    public String index() {
        return "indexHR.html";
    }


    @PostMapping("/upload")
    public ResponseEntity<?> uploadSample(
            @RequestParam(value = "user_id", required = false) String user_id,
            @RequestParam(value = "year", required = false) String year,
            @RequestParam("file_path") String file_path,
            @RequestParam("filename") String filename,
            @RequestParam("uploadFile") MultipartFile uploadFile) {

        try {
            if (user_id == null || user_id.trim().isEmpty() ||
                    year == null || year.trim().isEmpty() ||
                    file_path == null || file_path.trim().isEmpty() ||
                    filename == null || filename.trim().isEmpty() ||
                    uploadFile == null || uploadFile.isEmpty()) {

                throw new ItrIdValidationException("All fields must be filled.");
            }

            int parsedUserId;
            int parsedYear;

            try {
                parsedUserId = Integer.parseInt(user_id);
                parsedYear = Integer.parseInt(year);
            } catch (NumberFormatException e) {
                throw new UserIdValidationException("User ID and Year must be valid numbers.");
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

            Path directory = Paths.get(file_path);
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }

            LocalDateTime dateTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMyyyy_HHmmss");
            String timestampedFilename = String.format("%s_%s.pdf", filename, formatter.format(dateTime));

            Path targetFile = directory.resolve(timestampedFilename);
            Files.copy(uploadFile.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);

            String pdfPassword = PdfPasswordUtil.generatePassword();

            PdfPasswordUtil.encryptPdf(targetFile.toFile(), "OwnerSecretKey123", pdfPassword);

            if (upload.upload(parsedUserId, parsedYear, file_path, timestampedFilename, pdfPassword)) {
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(String.format("File %s saved successfully. PDF password: %s", timestampedFilename, pdfPassword));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to save record.");
            }

        } catch (UserIdValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("File save failed: " + e.getMessage());
        }
    }

    @PostMapping(value = "/update")
    public ResponseEntity<?> updateSample(
            @RequestParam(value = "itr_id", required = false) String itr_id,
            @RequestParam(value = "year", required = false) String year,
            @RequestParam("file_path") String file_path,
            @RequestParam("filename") String filename,
            @RequestParam("uploadFile") MultipartFile uploadFile) {

        if (itr_id == null || itr_id.trim().isEmpty() ||
                year == null || year.trim().isEmpty() ||
                file_path == null || file_path.trim().isEmpty() ||
                filename == null || filename.trim().isEmpty() ||
                uploadFile == null || uploadFile.isEmpty()) {

            throw new ItrIdValidationException("All fields must be filled.");
        }

        int parsedItrId, parsedYear;
        try {
            parsedItrId = Integer.parseInt(itr_id.trim());
            parsedYear = Integer.parseInt(year.trim());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("ITR ID and Year must be valid numbers.");
        }

        try {
            UploadModel oldRecord = uploadRepository.findById((long) parsedItrId).orElse(null);
            if (oldRecord == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("This user does not exist.");
            }

            int duplicates = uploadRepository.countByUserIdAndYearExcludingItr(
                    oldRecord.getUserId(), parsedYear, parsedItrId);
            if (duplicates > 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("This user already has an ITR for year " + parsedYear + ". Update rejected.");
            }

            if (!"application/pdf".equals(uploadFile.getContentType())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Invalid file type. Only PDF files are allowed.");
            }

            Path directory = Paths.get(file_path);
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }

            Path oldFilePath = Paths.get(oldRecord.getFilePath(), oldRecord.getFilename());
            try {
                Files.deleteIfExists(oldFilePath);
                System.out.println("Deleted old file: " + oldFilePath.toAbsolutePath());
            } catch (IOException ex) {
                System.out.println("Failed to delete old file: " + ex.getMessage());
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMyyyy_HHmmss"));
            String updatedFilename = String.format("%s_%s.pdf", filename, timestamp);
            Path targetFile = directory.resolve(updatedFilename);
            Files.copy(uploadFile.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);

            String pdfPassword = oldRecord.getPdfPassword();
            PdfPasswordUtil.encryptPdf(targetFile.toFile(), "OwnerSecret123", pdfPassword);

            boolean updated = upload.update(parsedItrId, parsedYear, file_path, updatedFilename);
            if (!updated) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Failed to update ITR record with ID: " + parsedItrId);
            }

            return ResponseEntity.ok("ITR record " + parsedItrId + " successfully updated.");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("File saving failed: " + e.getMessage());
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

        try {
            UploadModel record = uploadRepository.findById((long) parsedItrID).orElse(null);
            if (record != null) {
                Path fileToDelete = Paths.get(record.getFilePath(), record.getFilename());
                try {
                    Files.deleteIfExists(fileToDelete);
                    System.out.println("Deleted file from directory: " + fileToDelete.toAbsolutePath());
                } catch (IOException ex) {
                    System.out.println("Failed to delete file: " + ex.getMessage());
                }
            }

            if (!upload.remove(parsedItrID)) {
                return ResponseEntity.status(500)
                        .body(String.format("ITR number: %s is not removed from database.", parsedItrID));
            }

            return ResponseEntity.ok(String.format("ITR number: %s is removed, including its file.", parsedItrID));

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("Unexpected error while removing ITR record: " + e.getMessage());
        }
    }
}


