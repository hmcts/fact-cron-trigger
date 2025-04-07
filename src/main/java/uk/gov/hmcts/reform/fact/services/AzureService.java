package uk.gov.hmcts.reform.fact.services;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.fact.exceptions.AzureIOException;
import uk.gov.hmcts.reform.fact.models.StringMultipartFile;
import uk.gov.hmcts.reform.fact.utils.CsvUtil;

import java.io.IOException;

@Service
@Slf4j
public class AzureService {

    private final BlobServiceClient blobServiceClient;

    public AzureService(@Autowired BlobServiceClient blobServiceClient) {
        this.blobServiceClient = blobServiceClient;
    }

    public void createCsvFileAndUpload(String containerName, String blobName, JsonNode jsonNodeData) {
        try {
            uploadFile(containerName, blobName,
                       createCsvFile(blobName, new CsvUtil().convertJsonToCsv(jsonNodeData)));
        } catch (IOException ex) {
            throw new AzureIOException(ex.getMessage());
        }
    }

    public StringMultipartFile createCsvFile(String blobName, String csvString) {
        return new StringMultipartFile(
            blobName,
            blobName,
            "text/csv",
            csvString
        );
    }

    public void uploadFile(String containerName, String blobName, MultipartFile file) throws IOException {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        blobClient.upload(file.getInputStream(), file.getSize(), true);
        log.info("Uploaded file {} to {}", file.getOriginalFilename(), containerName);
    }
}
