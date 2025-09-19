package com.metrobank.uploadITR.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = Map.of(
                "status", "UP",
                "service", "UploadITR",
                "timestamp", LocalDateTime.now(),
                "version", "1.0.0",
                "features", Map.of(
                        "fileUpload", "enabled",
                        "azureStorage", "enabled",
                        "emailNotification", "enabled",
                        "pdfEncryption", "enabled"
                )
        );
        return ResponseEntity.ok(health);
    }
}