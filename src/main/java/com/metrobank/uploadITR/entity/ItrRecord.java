package com.metrobank.uploadITR.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "itr_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ItrRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "itr_id")
    private Long itrId;

    @NotNull(message = "User ID is required")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotNull(message = "Year is required")
    @Min(value = 2000, message = "Year must be at least 2000")
    @Max(value = 2100, message = "Year must not exceed 2100")
    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "filename")
    private String filename;

    @Column(name = "pdf_password")
    private String pdfPassword;

    @Builder.Default
    @Column(name = "status")
    private String status = "active";

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper method
    public String getFullFilePath() {
        return filePath != null && filename != null ? filePath + filename : filename;
    }

    public boolean isActive() {
        return "active".equalsIgnoreCase(status);
    }
}