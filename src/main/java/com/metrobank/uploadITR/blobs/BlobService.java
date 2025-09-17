package com.metrobank.uploadITR.blobs;

import com.azure.storage.blob.*;
import com.azure.storage.blob.models.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;

@Service
public class BlobService {

    private final BlobContainerClient containerClient;

    public BlobService(
            @Value("${azure.storage.connection-string}") String connectionString,
            @Value("${azure.storage.container-name}") String containerName) {

        BlobServiceClient serviceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();

        containerClient = serviceClient.getBlobContainerClient(containerName);

        if (!containerClient.exists()) {
            containerClient.create();
        }
    }

    // Upload file
    public void uploadFile(String blobName, InputStream data, long size) {
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        blobClient.upload(data, size, true);
    }


    public InputStream downloadFile(String blobName) {
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        return blobClient.openInputStream();
    }

    // Delete file
    public void deleteFile(String blobName) {
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        blobClient.deleteIfExists();
    }

    // Check if exists
    public boolean exists(String blobName) {
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        return blobClient.exists();
    }
}
