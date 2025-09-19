package com.metrobank.uploadITR.dto;

import jakarta.validation.constraints.*;
import lombok.*;

// Upload Request dto
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItrUploadRequest {
    @NotNull(message = "User ID is required")
    @Positive(message = "User ID must be positive")
    private Long userId;

    @NotNull(message = "Year is required")
    @Min(value = 2000, message = "Year must be at least 2000")
    @Max(value = 2100, message = "Year cannot be in the future beyond reasonable limits")
    private Integer year;

    @NotBlank(message = "Filename is required")
    @Size(max = 255, message = "Filename too long")
    private String filename;
}