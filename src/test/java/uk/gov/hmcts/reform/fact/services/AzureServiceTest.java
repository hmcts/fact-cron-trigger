package uk.gov.hmcts.reform.fact.services;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.azure.storage.blob.models.BlobProperties;
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
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
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

    @Test
    void shouldUploadPathWithMetadata() {
        Path path = Path.of("postcode-reference.csv");
        Map<String, String> metadata = Map.of("onspdItemId", "aug-item");
        when(blobServiceClient.getBlobContainerClient("onspd")).thenReturn(containerClient);
        when(containerClient.getBlobClient("postcode-reference.csv")).thenReturn(blobClient);

        azureService.uploadFile("onspd", "postcode-reference.csv", path, metadata);

        verify(containerClient).createIfNotExists();
        verify(blobClient).uploadFromFile(path.toString(), true);
        verify(blobClient).setMetadata(metadata);
    }

    @Test
    void shouldGetBlobMetadata() {
        BlobProperties blobProperties = mock(BlobProperties.class);
        when(blobServiceClient.getBlobContainerClient("onspd")).thenReturn(containerClient);
        when(containerClient.exists()).thenReturn(true);
        when(containerClient.getBlobClient("postcode-reference.csv")).thenReturn(blobClient);
        when(blobClient.exists()).thenReturn(true);
        when(blobClient.getProperties()).thenReturn(blobProperties);
        when(blobProperties.getMetadata()).thenReturn(Map.of("onspdItemId", "aug-item"));

        Optional<String> itemId = azureService.getBlobMetadata(
            "onspd", "postcode-reference.csv", "onspdItemId"
        );

        assertThat(itemId).contains("aug-item");
    }

    @Test
    void shouldReturnEmptyMetadataWhenContainerDoesNotExist() {
        when(blobServiceClient.getBlobContainerClient("onspd")).thenReturn(containerClient);
        when(containerClient.exists()).thenReturn(false);

        Optional<String> itemId = azureService.getBlobMetadata(
            "onspd", "postcode-reference.csv", "onspdItemId"
        );

        assertThat(itemId).isEmpty();
    }

    @Test
    void shouldReturnEmptyMetadataWhenBlobDoesNotExist() {
        when(blobServiceClient.getBlobContainerClient("onspd")).thenReturn(containerClient);
        when(containerClient.exists()).thenReturn(true);
        when(containerClient.getBlobClient("postcode-reference.csv")).thenReturn(blobClient);
        when(blobClient.exists()).thenReturn(false);

        Optional<String> itemId = azureService.getBlobMetadata(
            "onspd", "postcode-reference.csv", "onspdItemId"
        );

        assertThat(itemId).isEmpty();
    }
}
