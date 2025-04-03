package uk.gov.hmcts.reform.fact.config;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class AzureBlobConfiguration {
    @Value("${azure.blob.azure-connection-string}")
    private String connectionString;

    @Bean
    public BlobServiceClient blobServiceClient() {
        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
            .connectionString(connectionString)
            .buildClient();

        log.info("Blob service client created");
        log.info(String.valueOf(blobServiceClient.listBlobContainers().stream().count()));

        return blobServiceClient;
    }
}
