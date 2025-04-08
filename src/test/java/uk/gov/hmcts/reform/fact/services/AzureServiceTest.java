package uk.gov.hmcts.reform.fact.services;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.exceptions.AzureIOException;
import uk.gov.hmcts.reform.fact.models.StringMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AzureServiceTest {

    @Mock
    private BlobServiceClient blobServiceClient;

    @Mock
    private BlobContainerClient containerClient;

    @Mock
    private BlobClient blobClient;

    @InjectMocks
    private AzureService azureService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        azureService = new AzureService(blobServiceClient);
    }

    @Test
    void shouldCreateCsvFile() {
        String blobName = "test.csv";
        String csvContent = "col1,col2\nval1,val2";

        StringMultipartFile result = azureService.createCsvFile(blobName, csvContent);

        assertThat(result.getName()).isEqualTo(blobName);
        assertThat(result.getOriginalFilename()).isEqualTo(blobName);
        assertThat(result.getContentType()).isEqualTo("text/csv");
        assertThat(result.getBytes()).isEqualTo(csvContent.getBytes());
    }

    @Test
    void shouldUploadFileSuccessfully() throws IOException {
        String containerName = "test-container";
        String blobName = "test.csv";
        String fileContent = "col1,col2\nval1,val2";

        StringMultipartFile file = new StringMultipartFile(blobName, blobName, "text/csv", fileContent);

        when(blobServiceClient.getBlobContainerClient(containerName)).thenReturn(containerClient);
        when(containerClient.getBlobClient(blobName)).thenReturn(blobClient);

        azureService.uploadFile(containerName, blobName, file);

        verify(blobClient).upload(any(ByteArrayInputStream.class),
                                  eq((long) fileContent.getBytes().length), eq(true)
        );
    }

    @Test
    void shouldThrowAzureIOExceptionWhenCreateCsvFileAndUploadFails() throws Exception {
        String containerName = "container";
        String blobName = "test.csv";

        AzureService spyService = spy(azureService);

        StringMultipartFile realFile = new StringMultipartFile(blobName, blobName, "text/csv", "some,data");
        StringMultipartFile spyFile = spy(realFile);

        doThrow(new IOException("Stream failure")).when(spyFile).getInputStream();

        doReturn(spyFile).when(spyService).createCsvFile(eq(blobName), anyString());

        when(blobServiceClient.getBlobContainerClient(containerName)).thenReturn(containerClient);
        when(containerClient.getBlobClient(blobName)).thenReturn(blobClient);

        assertThatThrownBy(() -> spyService.createCsvFileAndUpload(
            containerName,
            blobName,
            objectMapper.readTree("{\"test\":\"value\"}")
        )).isInstanceOf(
            AzureIOException.class).hasMessageContaining("Stream failure");
    }
}
