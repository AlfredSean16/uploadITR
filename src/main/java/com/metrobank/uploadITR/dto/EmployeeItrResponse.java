package com.metrobank.uploadITR.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeItrResponse {
    private Long itrId;
    private Integer year;
    private String filename;
    private String status;
    private String uploadedDate;
    private boolean hasPassword;
    private boolean canDownload;
}