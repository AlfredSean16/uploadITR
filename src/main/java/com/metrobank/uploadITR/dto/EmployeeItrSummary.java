package com.metrobank.uploadITR.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeItrSummary {
    private String employeeName;
    private String employeeEmail;
    private Integer totalItrRecords;
    private List<Integer> availableYears;
    private List<EmployeeItrResponse> itrRecords;
}