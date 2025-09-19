package com.metrobank.uploadITR.service;

import com.azure.storage.blob.*;
import com.azure.storage.blob.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class BlobService {
    private static final Logger logger = LoggerFactory.getLogger(BlobService.class);

    private final BlobContainerClient containerClient;

    public BlobService(
            @Value("${azure.storage.connection-string}") String connectionString,
            @Value("${azure.storage.container-name}") String containerName) {

        try {
            BlobServiceClient serviceClient = new BlobServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();

            containerClient = serviceClient.getBlobContainerClient(containerName);

            if (!containerClient.exists()) {
                containerClient.create();
                logger.info("Created blob container: {}", containerName);
            }

            logger.info("Blob service initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize blob service: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize Azure Blob Storage", e);
        }
    }

    public String uploadFile(String blobName, InputStream data, long size) {
        try {
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            blobClient.upload(data, size, true);
            String blobUrl = blobClient.getBlobUrl();
            logger.info("File uploaded successfully to blob storage: {} at URL: {}", blobName, blobUrl);
            return blobUrl;
        } catch (Exception e) {
            logger.error("Failed to upload file to blob storage: {}", blobName, e);
            throw new RuntimeException("Failed to upload file: " + blobName, e);
        }
    }

    public InputStream downloadFile(String blobName) {
        try {
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            if (!blobClient.exists()) {
                throw new RuntimeException("File not found in blob storage: " + blobName);
            }
            logger.info("File downloaded successfully from blob storage: {}", blobName);
            return blobClient.openInputStream();
        } catch (Exception e) {
            logger.error("Failed to download file from blob storage: {}", blobName, e);
            throw new RuntimeException("Failed to download file: " + blobName, e);
        }
    }

    public void deleteFile(String blobName) {
        try {
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            boolean deleted = blobClient.deleteIfExists();
            if (deleted) {
                logger.info("File deleted successfully from blob storage: {}", blobName);
            } else {
                logger.warn("File not found for deletion in blob storage: {}", blobName);
            }
        } catch (Exception e) {
            logger.error("Failed to delete file from blob storage: {}", blobName, e);
            throw new RuntimeException("Failed to delete file: " + blobName, e);
        }
    }

    public boolean exists(String blobName) {
        try {
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            boolean exists = blobClient.exists();
            logger.debug("File existence check for {}: {}", blobName, exists);
            return exists;
        } catch (Exception e) {
            logger.error("Failed to check file existence in blob storage: {}", blobName, e);
            return false;
        }
    }

    public BlobProperties getFileProperties(String blobName) {
        try {
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            return blobClient.getProperties();
        } catch (Exception e) {
            logger.error("Failed to get file properties from blob storage: {}", blobName, e);
            throw new RuntimeException("Failed to get file properties: " + blobName, e);
        }
    }

    public String getContainerUrl() {
        return containerClient.getBlobContainerUrl();
    }

    public String getBlobFullPath(String blobName) {
        try {
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            return blobClient.getBlobUrl();
        } catch (Exception e) {
            logger.error("Failed to get blob URL for: {}", blobName, e);
            return containerClient.getBlobContainerUrl() + "/" + blobName;
        }
    }
}
