package uk.gov.hmcts.reform.fact.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.exceptions.OnspdImportException;
import uk.gov.hmcts.reform.fact.integrations.OnspdClient;
import uk.gov.hmcts.reform.fact.models.OnspdRelease;

import java.nio.file.Path;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnspdCsvServiceTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private OnspdClient onspdClient;

    @TempDir
    private Path temporaryDirectory;

    @Test
    void shouldCreateReducedCsvForLargeUserAndTerminatedPostcodes() throws Exception {
        OnspdRelease release = release();
        when(onspdClient.getRecordCount(release)).thenReturn(3L);
        when(onspdClient.getRecords(release, 0)).thenReturn(List.of(
            attributes(10, "G2  8GT", 201802, null, 1, 5, "55.858872", "-4.260375"),
            attributes(20, "AB1 0AA", 198001, 199606, 0, 1, "57.101459", "-2.242858")
        ));
        when(onspdClient.getRecords(release, 20)).thenReturn(List.of(
            attributes(35, "GY1 1AA", 199001, 202001, 1, 9, null, null)
        ));
        Path output = temporaryDirectory.resolve("postcode-reference.csv");

        long count = new OnspdCsvService(onspdClient).createReducedCsv(release, output);

        assertThat(count).isEqualTo(3);
        List<Map<String, String>> rows = readCsv(output);
        assertThat(rows).hasSize(3);
        assertThat(rows.get(0))
            .containsEntry("postcode", "G2 8GT")
            .containsEntry("status", "current")
            .containsEntry("is_large_user", "true")
            .containsEntry("has_coordinates", "true")
            .containsEntry("latitude", "55.858872")
            .containsEntry("longitude", "-4.260375");
        assertThat(rows.get(1))
            .containsEntry("status", "terminated")
            .containsEntry("is_large_user", "false")
            .containsEntry("terminated_yyyymm", "199606");
        assertThat(rows.get(2))
            .containsEntry("coordinate_quality", "9")
            .containsEntry("has_coordinates", "false")
            .containsEntry("latitude", "")
            .containsEntry("longitude", "");
    }

    @Test
    void shouldRejectEmptyFilteredDataset() {
        OnspdRelease release = release();
        when(onspdClient.getRecordCount(release)).thenReturn(0L);

        assertThatThrownBy(() -> new OnspdCsvService(onspdClient).createReducedCsv(
            release,
            temporaryDirectory.resolve("postcode-reference.csv")
        )).isInstanceOf(OnspdImportException.class)
            .hasMessageContaining("no large-user or terminated postcodes");
    }

    @Test
    void shouldRejectPaginationEndingBeforeExpectedCount() {
        OnspdRelease release = release();
        when(onspdClient.getRecordCount(release)).thenReturn(2L);
        when(onspdClient.getRecords(release, 0)).thenReturn(List.of(
            attributes(10, "G2 8GT", 201802, null, 1, 5, "55.858872", "-4.260375")
        ));
        when(onspdClient.getRecords(release, 10)).thenReturn(List.of());

        assertThatThrownBy(() -> new OnspdCsvService(onspdClient).createReducedCsv(
            release,
            temporaryDirectory.resolve("postcode-reference.csv")
        )).isInstanceOf(OnspdImportException.class)
            .hasMessageContaining("pagination ended after 1 of 2");
    }

    @Test
    void shouldRejectRecordsOutsideRequestedFilter() {
        OnspdRelease release = release();
        when(onspdClient.getRecordCount(release)).thenReturn(1L);
        when(onspdClient.getRecords(release, 0)).thenReturn(List.of(
            attributes(10, "AB1 0AA", 198001, null, 0, 1, "57.101459", "-2.242858")
        ));

        assertThatThrownBy(() -> new OnspdCsvService(onspdClient).createReducedCsv(
            release,
            temporaryDirectory.resolve("postcode-reference.csv")
        )).isInstanceOf(OnspdImportException.class)
            .hasMessageContaining("outside the requested filter");
    }

    @Test
    void shouldRejectNonAscendingObjectIds() {
        OnspdRelease release = release();
        when(onspdClient.getRecordCount(release)).thenReturn(2L);
        when(onspdClient.getRecords(release, 0)).thenReturn(List.of(
            attributes(10, "G2 8GT", 201802, null, 1, 5, "55.858872", "-4.260375"),
            attributes(10, "M60 9DJ", 201802, null, 1, 5, "53.480759", "-2.242631")
        ));

        assertThatThrownBy(() -> new OnspdCsvService(onspdClient).createReducedCsv(
            release,
            temporaryDirectory.resolve("postcode-reference.csv")
        )).isInstanceOf(OnspdImportException.class)
            .hasMessageContaining("ascending ObjectId order");
    }

    private List<Map<String, String>> readCsv(Path path) throws Exception {
        try (MappingIterator<Map<String, String>> rows = new CsvMapper()
            .readerFor(new TypeReference<Map<String, String>>() { })
            .with(CsvSchema.emptySchema().withHeader())
            .readValues(path.toFile())) {
            return rows.readAll();
        }
    }

    private JsonNode attributes(Integer objectId,
                                String postcode,
                                Integer introduced,
                                Integer terminated,
                                Integer userType,
                                Integer gridIndicator,
                                String latitude,
                                String longitude) {
        return OBJECT_MAPPER.valueToTree(Map.of(
            "ObjectId", objectId,
            "pcds", postcode,
            "dointr", introduced,
            "doterm", terminated == null ? OBJECT_MAPPER.nullNode() : terminated,
            "usrtypind", userType,
            "gridind", gridIndicator,
            "lat", latitude == null ? OBJECT_MAPPER.nullNode() : latitude,
            "long", longitude == null ? OBJECT_MAPPER.nullNode() : longitude
        ));
    }

    private OnspdRelease release() {
        return new OnspdRelease("aug-item", "August 2026", YearMonth.of(2026, 8), "https://services.test", 3L);
    }
}
