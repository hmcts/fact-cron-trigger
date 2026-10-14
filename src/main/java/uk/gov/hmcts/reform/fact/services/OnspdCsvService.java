package uk.gov.hmcts.reform.fact.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.exceptions.OnspdImportException;
import uk.gov.hmcts.reform.fact.integrations.OnspdClient;
import uk.gov.hmcts.reform.fact.models.OnspdRelease;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class OnspdCsvService {

    private static final CsvSchema OUTPUT_SCHEMA = CsvSchema.builder()
        .addColumn("postcode")
        .addColumn("status")
        .addColumn("is_large_user")
        .addColumn("introduced_yyyymm")
        .addColumn("terminated_yyyymm")
        .addColumn("coordinate_quality")
        .addColumn("has_coordinates")
        .addColumn("latitude")
        .addColumn("longitude")
        .setUseHeader(true)
        .build();

    private final OnspdClient onspdClient;
    private final CsvMapper csvMapper;

    public OnspdCsvService(OnspdClient onspdClient) {
        this.onspdClient = onspdClient;
        this.csvMapper = new CsvMapper();
    }

    public long createReducedCsv(OnspdRelease release, Path destination) {
        long expectedRecordCount = onspdClient.getRecordCount(release);
        if (expectedRecordCount < 1) {
            throw new OnspdImportException("ONSPD query returned no large-user or terminated postcodes");
        }

        long written = 0;
        long lastObjectId = 0;
        try (OutputStream outputStream = Files.newOutputStream(destination);
             SequenceWriter csvWriter = csvMapper
                 .writerFor(new TypeReference<Map<String, Object>>() { })
                 .with(OUTPUT_SCHEMA)
                 .writeValues(outputStream)) {

            while (written < expectedRecordCount) {
                List<JsonNode> records = onspdClient.getRecords(release, lastObjectId);
                if (records.isEmpty()) {
                    throw new OnspdImportException(
                        "ONSPD pagination ended after " + written + " of " + expectedRecordCount + " records"
                    );
                }

                for (JsonNode attributes : records) {
                    long objectId = requiredLong(attributes, "ObjectId");
                    if (objectId <= lastObjectId) {
                        throw new OnspdImportException("ONSPD records were not returned in ascending ObjectId order");
                    }
                    csvWriter.write(toOutputRow(attributes));
                    lastObjectId = objectId;
                }
                written += records.size();

                if (written % 100_000 < records.size()) {
                    log.info("Processed {} of {} ONSPD records", written, expectedRecordCount);
                }
            }
        } catch (IOException ex) {
            throw new OnspdImportException("Unable to create reduced ONSPD CSV", ex);
        }

        if (written != expectedRecordCount) {
            throw new OnspdImportException(
                "ONSPD record count changed during export: expected " + expectedRecordCount + " but wrote " + written
            );
        }

        return written;
    }

    private Map<String, Object> toOutputRow(JsonNode attributes) {
        String postcode = requiredText(attributes, "pcds").trim().replaceAll("\\s+", " ");
        final String introduced = requiredText(attributes, "dointr");
        String terminated = optionalText(attributes, "doterm");
        String userType = requiredText(attributes, "usrtypind");
        final String coordinateQuality = requiredText(attributes, "gridind");
        String latitude = optionalText(attributes, "lat");
        String longitude = optionalText(attributes, "long");

        boolean largeUser = "1".equals(userType);
        boolean isTerminated = terminated != null;
        if (!largeUser && !isTerminated) {
            throw new OnspdImportException("ONSPD source returned a row outside the requested filter: " + postcode);
        }

        boolean hasCoordinates = latitude != null && longitude != null;
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("postcode", postcode);
        row.put("status", isTerminated ? "terminated" : "current");
        row.put("is_large_user", largeUser);
        row.put("introduced_yyyymm", introduced);
        row.put("terminated_yyyymm", terminated);
        row.put("coordinate_quality", coordinateQuality);
        row.put("has_coordinates", hasCoordinates);
        row.put("latitude", latitude);
        row.put("longitude", longitude);
        return row;
    }

    private String requiredText(JsonNode attributes, String field) {
        String value = optionalText(attributes, field);
        if (value == null) {
            throw new OnspdImportException("ONSPD source row is missing required field " + field);
        }
        return value;
    }

    private long requiredLong(JsonNode attributes, String field) {
        JsonNode value = attributes.path(field);
        if (!value.canConvertToLong()) {
            throw new OnspdImportException("ONSPD source row is missing required field " + field);
        }
        return value.asLong();
    }

    private String optionalText(JsonNode attributes, String field) {
        JsonNode value = attributes.path(field);
        return value.isMissingNode() || value.isNull() || value.asText().isBlank() ? null : value.asText();
    }
}
