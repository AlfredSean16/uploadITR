package com.metrobank.uploadITR.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItrDownloadRequest {
    private Long itrId;
    private String password; // PDF password provided by employee
}