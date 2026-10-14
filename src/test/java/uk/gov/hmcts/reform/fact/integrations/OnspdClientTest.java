package uk.gov.hmcts.reform.fact.integrations;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.fact.config.OnspdProperties;
import uk.gov.hmcts.reform.fact.exceptions.OnspdImportException;
import uk.gov.hmcts.reform.fact.models.OnspdRelease;

import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OnspdClientTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private OnspdProperties properties;
    private OnspdClient client;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        properties = new OnspdProperties();
        properties.setCatalogueUrl("https://catalogue.test/search");
        properties.setAllowedServiceHost("services.test");
        properties.setPageSize(2);
        properties.setMaxAttempts(2);
        properties.setInitialRetryBackoffMillis(0);
        client = new OnspdClient(restTemplate, properties);
    }

    @Test
    void shouldFindLatestOnspdHostedTableByReleaseDate() {
        server.expect(once(), requestTo(org.hamcrest.Matchers.startsWith("https://catalogue.test/search")))
            .andExpect(request -> assertThat(request.getURI().getQuery()).contains(OnspdClient.CATALOGUE_QUERY))
            .andRespond(withSuccess("""
                {
                  "results": [
                    {
                      "id": "correction",
                      "title": "Corrections to Northern Ireland postcodes",
                      "url": "https://services.test/correction",
                      "modified": 1
                    },
                    {
                      "id": "may-item",
                      "title": "ONS Postcode Directory (May 2026) for the United Kingdom (Hosted Table)",
                      "url": "https://services.test/may/FeatureServer",
                      "modified": 2
                    },
                    {
                      "id": "aug-item-old",
                      "title": "ONS Postcode Directory (August 2026) for the United Kingdom (Hosted Table)",
                      "url": "https://services.test/aug-old/FeatureServer",
                      "modified": 2
                    },
                    {
                      "id": "aug-item",
                      "title": "ONS Postcode Directory (August 2026) for the United Kingdom (Hosted Table)",
                      "url": "https://services.test/aug/FeatureServer",
                      "modified": 3
                    }
                  ]
                }
                """, MediaType.APPLICATION_JSON));

        OnspdRelease release = client.findLatestRelease();

        assertThat(release.itemId()).isEqualTo("aug-item");
        assertThat(release.releaseDate()).isEqualTo(YearMonth.of(2026, 8));
        assertThat(release.serviceUrl()).isEqualTo("https://services.test/aug/FeatureServer");
        server.verify();
    }

    @Test
    void shouldRejectCatalogueWithoutOnspdHostedTable() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("https://catalogue.test/search")))
            .andRespond(withSuccess("{\"results\":[]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(client::findLatestRelease)
            .isInstanceOf(OnspdImportException.class)
            .hasMessageContaining("No ONSPD hosted-table release");
        server.verify();
    }

    @Test
    void shouldGetFilteredRecordCount() {
        OnspdRelease release = release("https://services.test/aug/FeatureServer");
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/0/query")))
            .andExpect(request -> assertThat(request.getURI().getQuery())
                .contains(OnspdClient.POSTCODE_SELECTION_FILTER))
            .andExpect(queryParam("returnCountOnly", "true"))
            .andRespond(withSuccess("{\"count\":993979}", MediaType.APPLICATION_JSON));

        assertThat(client.getFilteredPostcodeCount(release)).isEqualTo(993_979);
        server.verify();
    }

    @Test
    void shouldRejectInvalidRecordCount() {
        OnspdRelease release = release("https://services.test/aug/FeatureServer");
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/0/query")))
            .andRespond(withSuccess("{\"count\":\"invalid\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getFilteredPostcodeCount(release))
            .isInstanceOf(OnspdImportException.class)
            .hasMessageContaining("valid count");
        server.verify();
    }

    @Test
    void shouldGetOnlyRequiredFieldsForPage() {
        OnspdRelease release = release("https://services.test/aug/FeatureServer");
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/0/query")))
            .andExpect(queryParam("outFields", OnspdClient.POSTCODE_FIELDS))
            .andExpect(request -> assertThat(request.getURI().getQuery())
                .contains("(" + OnspdClient.POSTCODE_SELECTION_FILTER + ") AND ObjectId > 2"))
            .andExpect(queryParam("resultRecordCount", "2"))
            .andRespond(withSuccess("""
                {"features":[{"attributes":{"pcds":"G2 8GT","usrtypind":1}}]}
                """, MediaType.APPLICATION_JSON));

        List<JsonNode> records = client.getPostcodeRecordsAfter(release, 2);

        assertThat(records).hasSize(1);
        assertThat(records.getFirst().path("pcds").asText()).isEqualTo("G2 8GT");
        server.verify();
    }

    @Test
    void shouldRetryArcGisErrorResponse() {
        OnspdRelease release = release("https://services.test/aug/FeatureServer");
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/0/query")))
            .andRespond(withSuccess(
                "{\"error\":{\"code\":500,\"message\":\"Database error\"}}",
                MediaType.APPLICATION_JSON
            ));
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/0/query")))
            .andRespond(withSuccess("{\"features\":[]}", MediaType.APPLICATION_JSON));

        assertThat(client.getPostcodeRecordsAfter(release, 300_000)).isEmpty();
        server.verify();
    }

    @Test
    void shouldIncludeArcGisErrorDetailsAfterRetriesAreExhausted() {
        OnspdRelease release = release("https://services.test/aug/FeatureServer");
        String error = """
            {"error":{"code":500,"message":"Database error","details":["Try again later"]}}
            """;
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/0/query")))
            .andRespond(withSuccess(error, MediaType.APPLICATION_JSON));
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/0/query")))
            .andRespond(withSuccess(error, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getPostcodeRecordsAfter(release, 300_000))
            .isInstanceOf(OnspdImportException.class)
            .hasMessageContaining("code 500")
            .hasMessageContaining("Try again later");
        server.verify();
    }

    @Test
    void shouldRejectUnexpectedServiceHost() {
        OnspdRelease release = release("https://unexpected.test/FeatureServer");

        assertThatThrownBy(() -> client.getFilteredPostcodeCount(release))
            .isInstanceOf(OnspdImportException.class)
            .hasMessageContaining("unexpected host");
    }

    private OnspdRelease release(String serviceUrl) {
        return new OnspdRelease("aug-item", "August 2026", YearMonth.of(2026, 8), serviceUrl, 3L);
    }
}
