package uk.gov.hmcts.reform.fact.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ResourceUtils;
import uk.gov.hmcts.reform.fact.exceptions.JsonConvertException;
import uk.gov.hmcts.reform.fact.factapi.FactClient;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FactServiceTest {

    @Mock
    private FactClient factClient;

    @InjectMocks
    private FactService factService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        factService = new FactService(factClient);
    }

    @Test
    void shouldReturnJsonNodeWhenFactClientReturnsValidJson() throws IOException {
        File jsonFile = ResourceUtils.getFile("classpath:court_test_data.json");
        JsonNode expectedJsonNode = objectMapper.readTree(jsonFile);

        when(factClient.getAllCourtData()).thenReturn(expectedJsonNode.toString());
        JsonNode result = factService.getCourtData();
        assertThat(result).isEqualTo(expectedJsonNode);

        for (int i = 0; i < result.size(); i++) {
            JsonNode court = result.get(i);

            String courtName = court.get("name").asText();
            assertThat(courtName).isNotBlank();

            if (court.has("lat") && !court.get("lat").isNull()) {
                assertThat(court.get("lat").isDouble()).isTrue();
            } else {
                assertThat(court.get("lat").isNull()).isTrue();
            }

            if (court.has("lon") && !court.get("lon").isNull()) {
                assertThat(court.get("lon").isDouble()).isTrue();
            } else {
                assertThat(court.get("lon").isNull()).isTrue();
            }

            assertThat(court.get("displayed").asBoolean()).isIn(true, false); // Ensure it's either true or false
            assertThat(court.get("hideAols").asBoolean()).isIn(true, false);

            if (court.has("dxNumber")) {
                assertThat(court.get("dxNumber").asText()).isNotBlank();
            } else {
                assertThat(court.get("dxNumber").isNull()).isTrue();
            }

            if (court.has("distance")) {
                if (!court.get("distance").isNull()) {
                    assertThat(court.get("distance").isDouble()).isTrue();
                }
            } else {
                assertThat(court.get("distance").isNull()).isTrue();
            }

            // Addresses checks
            JsonNode addressesNode = court.get("addresses");
            assertThat(addressesNode.isArray()).isTrue();
            for (JsonNode address : addressesNode) {
                if (address.has("addressLines")) {
                    assertThat(address.get("addressLines").isArray()).isTrue();
                    if (!address.get("addressLines").isEmpty()) {
                        assertThat(address.get("addressLines").get(0).asText()).isNotBlank();
                    }
                }

                assertThat(address.has("postcode") ? address.get("postcode").asText() : "").isNotNull();
                assertThat(address.has("town") ? address.get("town").asText() : "").isNotNull();
                assertThat(address.has("county") ? address.get("county").asText() : "").isNotNull();
                assertThat(address.has("type")).isTrue();
                assertThat(address.has("description")).isTrue();
                assertThat(address.has("epimId")).isTrue();
                assertThat(address.has("fieldsOfLaw")).isTrue();
            }

            // Areas of Law checks
            JsonNode areasOfLawNode = court.get("areasOfLaw");
            if (areasOfLawNode != null && areasOfLawNode.isArray()) {
                for (JsonNode areaOfLaw : areasOfLawNode) {
                    assertThat(areaOfLaw.has("name")).isTrue();
                    assertThat(areaOfLaw.has("externalLink")).isTrue();
                    assertThat(areaOfLaw.has("displayUrl")).isTrue();
                    assertThat(areaOfLaw.has("externalLinkDesc")).isTrue();
                    assertThat(areaOfLaw.has("displayName")).isTrue();
                    assertThat(areaOfLaw.has("displayExternalLink")).isTrue();
                }
            }
        }
    }

    @Test
    void shouldThrowJsonConvertExceptionWhenInvalidJsonIsReturned() {
        // Given
        String invalidJson = "{invalidJson";  // Malformed JSON

        when(factClient.getAllCourtData()).thenReturn(invalidJson);

        // When & Then
        assertThatThrownBy(() -> factService.getCourtData())
            .isInstanceOf(JsonConvertException.class)
            .hasMessageContaining("Error converting JSON to court model");
    }
}
