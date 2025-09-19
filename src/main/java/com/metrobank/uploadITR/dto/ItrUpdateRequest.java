package com.metrobank.uploadITR.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItrUpdateRequest {
    @NotNull(message = "ITR ID is required")
    @Positive(message = "ITR ID must be positive")
    private Long itrId;

    @NotNull(message = "Year is required")
    @Min(value = 2000, message = "Year must be at least 2000")
    @Max(value = 2100, message = "Year cannot be in the future beyond reasonable limits")
    private Integer year;

    @NotBlank(message = "Filename is required")
    @Size(max = 255, message = "Filename too long")
    private String filename;
}