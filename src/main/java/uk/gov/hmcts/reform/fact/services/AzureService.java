package uk.gov.hmcts.reform.fact.services;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@Slf4j
public class AzureService {

    private final BlobServiceClient blobServiceClient;

    public AzureService(@Autowired BlobServiceClient blobServiceClient) {
        this.blobServiceClient = blobServiceClient;
    }

    public void uploadFile(String containerName, String blobName, MultipartFile file) throws IOException {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        blobClient.upload(file.getInputStream(), file.getSize(), true);
        log.info("Uploaded file {} to {}", file.getOriginalFilename(), containerName);
    }
}
