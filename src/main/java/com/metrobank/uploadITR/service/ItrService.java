package com.metrobank.uploadITR.service;

import com.metrobank.uploadITR.dto.ItrResponse;
import com.metrobank.uploadITR.entity.ItrRecord;
import com.metrobank.uploadITR.exception.BusinessException;
import com.metrobank.uploadITR.exception.ResourceNotFoundException;
import com.metrobank.uploadITR.repository.ItrRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ItrService {
    private static final Logger logger = LoggerFactory.getLogger(ItrService.class);

    private final ItrRepository itrRepository;

    public ItrService(ItrRepository itrRepository) {
        this.itrRepository = itrRepository;
    }

    public ItrRecord createItrRecord(Long userId, Integer year, String blobUrl, String filename, String pdfPassword) {
        // Validate user exists
        if (itrRepository.countUserById(userId) == 0) {
            throw new BusinessException("User with ID " + userId + " does not exist");
        }

        // Check for future year
        int currentYear = LocalDateTime.now(ZoneId.of("UTC")).getYear();
        if (year > currentYear) {
            throw new BusinessException("Cannot upload ITR for future year: " + year);
        }

        // Check for existing ITR for the same user and year
        if (itrRepository.countActiveByUserIdAndYear(userId, year) > 0) {
            throw new BusinessException("ITR already exists for user " + userId + " and year " + year);
        }

        ItrRecord itrRecord = ItrRecord.builder()
                .userId(userId)
                .year(year)
                .filePath(blobUrl) // Store the full Azure Blob Storage URL
                .filename(filename)
                .pdfPassword(pdfPassword)
                .status("active")
                .build();

        ItrRecord saved = itrRepository.save(itrRecord);
        logger.info("Created ITR record for user {} and year {} with blob URL: {}", userId, year, blobUrl);

        return saved;
    }

    public ItrRecord updateItrRecord(Long itrId, Integer year, String blobUrl, String filename, String pdfPassword) {
        ItrRecord existingRecord = itrRepository.findActiveById(itrId)
                .orElseThrow(() -> new ResourceNotFoundException("ITR record not found or inactive: " + itrId));

        // Check for future year
        int currentYear = LocalDateTime.now(ZoneId.of("UTC")).getYear();
        if (year > currentYear) {
            throw new BusinessException("Cannot update ITR for future year: " + year);
        }

        // Check for duplicates when changing year
        if (!existingRecord.getYear().equals(year)) {
            int duplicates = itrRepository.countDuplicateForUpdate(existingRecord.getUserId(), year, itrId);
            if (duplicates > 0) {
                throw new BusinessException("ITR already exists for user " + existingRecord.getUserId() + " and year " + year);
            }
        }

        int updatedRows = itrRepository.updateItrRecord(itrId, year, blobUrl, filename, pdfPassword);
        if (updatedRows == 0) {
            throw new BusinessException("Failed to update ITR record: " + itrId);
        }

        logger.info("Updated ITR record {} for user {} and year {} with blob URL: {}", itrId, existingRecord.getUserId(), year, blobUrl);

        // Return updated record
        return itrRepository.findActiveById(itrId)
                .orElseThrow(() -> new ResourceNotFoundException("ITR record not found after update: " + itrId));
    }

    public boolean removeItrRecord(Long itrId) {
        ItrRecord existingRecord = itrRepository.findActiveById(itrId)
                .orElseThrow(() -> new ResourceNotFoundException("ITR record not found or already inactive: " + itrId));

        int deletedRows = itrRepository.softDeleteById(itrId);
        boolean success = deletedRows > 0;

        if (success) {
            logger.info("Soft deleted ITR record {} for user {}", itrId, existingRecord.getUserId());
        }

        return success;
    }

    @Transactional(readOnly = true)
    public List<ItrResponse> getAllActiveItrRecords() {
        return itrRepository.findAllActive().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ItrResponse> getItrRecordsByUserId(Long userId) {
        // Validate user exists
        if (itrRepository.countUserById(userId) == 0) {
            throw new BusinessException("User with ID " + userId + " does not exist");
        }

        return itrRepository.findActiveByUserId(userId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ItrResponse getItrRecordById(Long itrId) {
        ItrRecord record = itrRepository.findActiveById(itrId)
                .orElseThrow(() -> new ResourceNotFoundException("ITR record not found: " + itrId));

        return convertToResponse(record);
    }

    @Transactional(readOnly = true)
    public String getUserEmail(Long userId) {
        return itrRepository.findUserEmailById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User email not found for ID: " + userId));
    }

    @Transactional(readOnly = true)
    public String getUserName(Long userId) {
        return itrRepository.findUserNameById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User name not found for ID: " + userId));
    }

    @Transactional(readOnly = true)
    public boolean userExists(Long userId) {
        return itrRepository.countUserById(userId) > 0;
    }

    private ItrResponse convertToResponse(ItrRecord record) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return ItrResponse.builder()
                .itrId(record.getItrId())
                .userId(record.getUserId())
                .year(record.getYear())
                .filename(record.getFilename())
                .filePath(record.getFilePath()) // This now contains the full Azure Blob Storage URL
                .status(record.getStatus())
                .createdAt(record.getCreatedAt() != null ? record.getCreatedAt().format(formatter) : null)
                .updatedAt(record.getUpdatedAt() != null ? record.getUpdatedAt().format(formatter) : null)
                .hasPassword(record.getPdfPassword() != null && !record.getPdfPassword().isEmpty())
                .build();
    }
}