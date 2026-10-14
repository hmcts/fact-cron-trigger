package uk.gov.hmcts.reform.fact.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.config.OnspdProperties;
import uk.gov.hmcts.reform.fact.integrations.OnspdClient;
import uk.gov.hmcts.reform.fact.models.OnspdRelease;
import uk.gov.hmcts.reform.fact.models.OnspdUpdateResult;

import java.nio.file.Path;
import java.time.YearMonth;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnspdServiceTest {

    @Mock
    private OnspdClient onspdClient;

    @Mock
    private OnspdCsvService onspdCsvService;

    @Mock
    private AzureService azureService;

    private OnspdService onspdService;
    private OnspdRelease release;

    @BeforeEach
    void setUp() {
        OnspdProperties properties = new OnspdProperties();
        properties.setContainerName("onspd");
        properties.setBlobName("postcode-reference.csv");
        onspdService = new OnspdService(onspdClient, onspdCsvService, azureService, properties);
        release = new OnspdRelease(
            "aug-item",
            "ONS Postcode Directory (August 2026) (Hosted Table)",
            YearMonth.of(2026, 8),
            "https://services.test/FeatureServer",
            3L
        );
    }

    @Test
    void shouldDoNothingWhenLatestReleaseIsAlreadyUploaded() {
        when(onspdClient.findLatestRelease()).thenReturn(release);
        when(azureService.getBlobMetadata("onspd", "postcode-reference.csv", OnspdService.SOURCE_VERSION_METADATA))
            .thenReturn(Optional.of("aug-item:3"));

        OnspdUpdateResult result = onspdService.updateIfRequired();

        assertThat(result.updated()).isFalse();
        assertThat(result.release()).isEqualTo("2026-08");
        verify(onspdCsvService, never()).createReducedCsv(any(), any());
        verify(azureService, never()).uploadFile(any(), any(), any(Path.class), any());
    }

    @Test
    void shouldCreateAndUploadCsvForNewRelease() {
        when(onspdClient.findLatestRelease()).thenReturn(release);
        when(azureService.getBlobMetadata("onspd", "postcode-reference.csv", OnspdService.SOURCE_VERSION_METADATA))
            .thenReturn(Optional.of("aug-item:2"));
        when(onspdCsvService.createReducedCsv(eq(release), any(Path.class))).thenReturn(993_979L);

        OnspdUpdateResult result = onspdService.updateIfRequired();

        assertThat(result.updated()).isTrue();
        assertThat(result.recordCount()).isEqualTo(993_979);
        assertThat(result.blobPath()).isEqualTo("onspd/postcode-reference.csv");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> metadata = ArgumentCaptor.forClass(Map.class);
        verify(azureService).uploadFile(
            eq("onspd"),
            eq("postcode-reference.csv"),
            any(Path.class),
            metadata.capture()
        );
        assertThat(metadata.getValue())
            .containsEntry(OnspdService.ITEM_ID_METADATA, "aug-item")
            .containsEntry(OnspdService.SOURCE_VERSION_METADATA, "aug-item:3")
            .containsEntry(OnspdService.RELEASE_METADATA, "2026-08")
            .containsEntry(OnspdService.RECORD_COUNT_METADATA, "993979");
    }
}
