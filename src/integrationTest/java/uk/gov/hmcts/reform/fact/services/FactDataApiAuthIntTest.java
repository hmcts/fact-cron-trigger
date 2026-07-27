package uk.gov.hmcts.reform.fact.services;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.reform.fact.config.AzureAdProperties;
import uk.gov.hmcts.reform.fact.config.FactDataApiFeignConfiguration;

import java.util.Collection;
import java.util.Collections;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = FactDataApiAuthIntTest.TestConfig.class)
class FactDataApiAuthIntTest {
    private static final String LIVE_TEST_FLAG = "fact.auth.live-test.enabled";
    private static final String LIVE_TEST_ENV_FLAG = "FACT_AUTH_LIVE_TEST_ENABLED";

    @Autowired
    private AzureAdTokenService azureAdTokenService;

    @Autowired
    private RequestInterceptor bearerTokenRequestInterceptor;

    @Autowired
    private AzureAdProperties azureAdProperties;

    @Value("${fact-data-api.url}")
    private String factDataApiUrl;

    @Value("${fact-data-api.endpoint.delete-users}")
    private String deleteUsersEndpoint;

    @Value("${fact.auth.live-test.enabled:false}")
    private boolean liveTestEnabledFromSpring;

    @BeforeEach
    void requireAzureAdSecrets() {
        Assumptions.assumeTrue(
            StringUtils.hasText(azureAdProperties.getClientId())
                && StringUtils.hasText(azureAdProperties.getClientSecret())
                && StringUtils.hasText(azureAdProperties.getScope()),
            "Set azure.ad.client-id, azure.ad.client-secret and azure.ad.scope "
                + "to run FactDataApiAuthIntTest"
        );
    }

    @Test
    void shouldFetchAzureAdAccessTokenForFactDataApiCalls() {
        String accessToken = azureAdTokenService.getAccessToken();
        assertThat(accessToken).isNotBlank();
    }

    @Test
    void shouldAddBearerAuthorizationHeaderToFactDataApiRequest() {
        RequestTemplate requestTemplate = new RequestTemplate();

        bearerTokenRequestInterceptor.apply(requestTemplate);

        Collection<String> authHeaders = requestTemplate.headers()
            .getOrDefault("Authorization", Collections.emptyList());
        assertThat(authHeaders).hasSize(1);
        assertThat(authHeaders.iterator().next()).startsWith("Bearer ");
    }

    @Test
    void shouldCallFactDataApiWithValidTokenOnLocalOnly() throws Exception {
        assumeLiveApiTestsEnabledAndSafeToRun();

        String accessToken = azureAdTokenService.getAccessToken();
        int status = callDeleteUsersApi(accessToken);

        assertThat(status)
            .as("Expected valid token to be accepted by fact-data-api")
            .isBetween(200, 299);
    }

    @Test
    void shouldRejectInvalidTokenOnLocalOnly() throws Exception {
        assumeLiveApiTestsEnabledAndSafeToRun();

        int status = callDeleteUsersApi("invalid-token");

        assertThat(status)
            .as("Expected invalid token to be rejected by fact-data-api")
            .isIn(401, 403);
    }

    private void assumeLiveApiTestsEnabledAndSafeToRun() {
        Assumptions.assumeTrue(
            isLiveTestEnabled(),
            "Set -D" + LIVE_TEST_FLAG + "=true or " + LIVE_TEST_ENV_FLAG + "=true to run live API auth tests"
        );

        Assumptions.assumeTrue(!isCiEnvironment(), "Live API auth tests are blocked on CI/pipeline");

        Assumptions.assumeTrue(
            isLocalUrl(factDataApiUrl),
            "Live API auth tests are local-only and require fact-data-api.url to point to localhost/127.0.0.1"
        );
    }

    private int callDeleteUsersApi(String token) throws Exception {
        Assumptions.assumeTrue(
            isLiveTestEnabled() && !isCiEnvironment() && isLocalUrl(factDataApiUrl),
            "DELETE live API auth tests are local-only and blocked on CI/pipeline"
        );

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(factDataApiUrl + deleteUsersEndpoint))
            .header("Authorization", "Bearer " + token)
            .DELETE()
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode();
    }

    private boolean isLiveTestEnabled() {
        return liveTestEnabledFromSpring
            || Boolean.getBoolean(LIVE_TEST_FLAG)
            || Boolean.parseBoolean(System.getenv(LIVE_TEST_ENV_FLAG));
    }

    private boolean isCiEnvironment() {
        return hasEnv("CI")
            || hasEnv("JENKINS_URL")
            || hasEnv("BUILD_ID")
            || hasEnv("TF_BUILD")
            || hasEnv("GITHUB_ACTIONS");
    }

    private boolean hasEnv(String name) {
        return StringUtils.hasText(System.getenv(name));
    }

    private boolean isLocalUrl(String url) {
        try {
            String host = URI.create(url).getHost();
            return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    @Configuration
    @EnableAutoConfiguration
    @EnableConfigurationProperties(AzureAdProperties.class)
    @Import({AzureAdTokenService.class, FactDataApiFeignConfiguration.class})
    static class TestConfig {
        // Minimal context for auth wiring checks.
    }
}




