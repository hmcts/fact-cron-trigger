package uk.gov.hmcts.reform.fact.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.config.OnspdProperties;
import uk.gov.hmcts.reform.fact.exceptions.OnspdImportException;
import uk.gov.hmcts.reform.fact.integrations.OnspdClient;
import uk.gov.hmcts.reform.fact.models.OnspdRelease;
import uk.gov.hmcts.reform.fact.models.OnspdUpdateResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class OnspdService {

    static final String ITEM_ID_METADATA = "onspdItemId";
    static final String SOURCE_VERSION_METADATA = "onspdSourceVersion";
    static final String RELEASE_METADATA = "onspdRelease";
    static final String RECORD_COUNT_METADATA = "recordCount";

    private static final DateTimeFormatter RELEASE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final OnspdClient onspdClient;
    private final OnspdCsvService onspdCsvService;
    private final AzureService azureService;
    private final OnspdProperties properties;

    public OnspdService(OnspdClient onspdClient,
                        OnspdCsvService onspdCsvService,
                        AzureService azureService,
                        OnspdProperties properties) {
        this.onspdClient = onspdClient;
        this.onspdCsvService = onspdCsvService;
        this.azureService = azureService;
        this.properties = properties;
    }

    public OnspdUpdateResult updateIfRequired() {
        OnspdRelease latestRelease = onspdClient.findLatestRelease();
        String release = latestRelease.releaseDate().format(RELEASE_FORMAT);
        String blobPath = properties.getContainerName() + "/" + properties.getBlobName();

        Optional<String> importedSourceVersion = azureService.getBlobMetadata(
            properties.getContainerName(),
            properties.getBlobName(),
            SOURCE_VERSION_METADATA
        );
        if (importedSourceVersion.filter(latestRelease.sourceVersion()::equals).isPresent()) {
            log.info("ONSPD release {} is already stored at {}", release, blobPath);
            return OnspdUpdateResult.unchanged(release, blobPath);
        }

        Path temporaryCsv = createTemporaryFile();
        try {
            long recordCount = onspdCsvService.createReducedCsv(latestRelease, temporaryCsv);
            azureService.uploadFile(
                properties.getContainerName(),
                properties.getBlobName(),
                temporaryCsv,
                Map.of(
                    ITEM_ID_METADATA, latestRelease.itemId(),
                    SOURCE_VERSION_METADATA, latestRelease.sourceVersion(),
                    RELEASE_METADATA, release,
                    RECORD_COUNT_METADATA, Long.toString(recordCount)
                )
            );
            return OnspdUpdateResult.updated(release, recordCount, blobPath);
        } finally {
            deleteTemporaryFile(temporaryCsv);
        }
    }

    private Path createTemporaryFile() {
        try {
            return Files.createTempFile("onspd-postcode-reference-", ".csv");
        } catch (IOException ex) {
            throw new OnspdImportException("Unable to create temporary ONSPD CSV", ex);
        }
    }

    private void deleteTemporaryFile(Path temporaryCsv) {
        try {
            Files.deleteIfExists(temporaryCsv);
        } catch (IOException ex) {
            log.warn("Unable to delete temporary ONSPD CSV {}", temporaryCsv, ex);
        }
    }
}
