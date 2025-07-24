package com.metrobank.uploadITR.DTO;

import jakarta.persistence.Column;



public class UploadDTO {

    private Integer userId;
    private Integer year;
    private String filePath;
    private String filename;

    public UploadDTO(Integer userId, Integer year, String filePath, String filename) {
        this.userId = userId;
        this.year = year;
        this.filePath = filePath;
        this.filename = filename;
    }

    public UploadDTO() {
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
}
