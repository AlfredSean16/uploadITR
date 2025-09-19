package com.metrobank.uploadITR.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItrResponse {
    private Long itrId;
    private Long userId;
    private Integer year;
    private String filename;
    private String filePath; // This will now contain the full Azure Blob Storage URL
    private String status;
    private String createdAt;
    private String updatedAt;
    private boolean hasPassword;
}
