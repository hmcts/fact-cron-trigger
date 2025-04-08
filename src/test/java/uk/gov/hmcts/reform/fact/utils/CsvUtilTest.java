package uk.gov.hmcts.reform.fact.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.exceptions.JsonConvertException;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CsvUtilTest {

    @Mock
    private CsvMapper mockCsvMapper;

    @InjectMocks
    private CsvUtil csvUtil;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @ParameterizedTest
    @MethodSource("uk.gov.hmcts.reform.fact.utils.CsvUtilTest#jsonTestCases")
    void shouldConvertJsonToCsvAndMatchExpectedContent(String jsonResourcePath, String... expectedContents)
        throws Exception {

        csvUtil = new CsvUtil();
        JsonNode jsonArray = readJsonFromFile(jsonResourcePath);
        String csv = csvUtil.convertJsonToCsv(jsonArray);

        for (String content : expectedContents) {
            assertThat(csv).contains(content);
        }
    }

    @ParameterizedTest
    @MethodSource("uk.gov.hmcts.reform.fact.utils.CsvUtilTest#jsonInvalidTestCases")
    void shouldThrowJsonConvertExceptionWhenCsvWriterFails(String jsonResourcePath) throws Exception {
        csvUtil = new CsvUtil(mockCsvMapper);
        JsonNode mockJsonNode = readJsonFromFile(jsonResourcePath);

        when(mockCsvMapper.writer(any(CsvSchema.class)))
            .thenThrow(new RuntimeException("Simulated write failure"));

        JsonConvertException exception = assertThrows(JsonConvertException.class, () -> {
            csvUtil.convertJsonToCsv(mockJsonNode);
        });

        assertEquals("Failed to convert JSON to CSV: Simulated write failure", exception.getMessage());
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> jsonTestCases() {
        return Stream.of(
            org.junit.jupiter.params.provider.Arguments.of(
                "src/test/resources/full_valid.json",
                new String[]{"Court A", "Criminal | Family", "Town: London, Postcode: EC1A 1BB, Address: Line 1, "
                        + "Line 2, Type: Main, County: London, Areas of Law: Criminal, Courts: Court A, Description: "
                        + "Main courthouse, EPIM ID: 12345"}
            ),
            org.junit.jupiter.params.provider.Arguments.of(
                "src/test/resources/missing_fields.json",
                new String[]{"Court Without Codes", "No areas of law available", "No address available"}
            ),
            org.junit.jupiter.params.provider.Arguments.of(
                "src/test/resources/empty_array.json",
                new String[]{"name,lat,lon,number,cci_code,magistrate_code"}
            ),
            org.junit.jupiter.params.provider.Arguments.of(
                "src/test/resources/multiple_entries.json",
                new String[]{"Court A", "Court B"}
            )
        );
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> jsonInvalidTestCases() {
        return Stream.of(
            org.junit.jupiter.params.provider.Arguments.of("src/test/resources/invalid_structure.json")
        );
    }

    private static JsonNode readJsonFromFile(String path) throws Exception {
        return objectMapper.readTree(Files.readString(Paths.get(path)));
    }
}
