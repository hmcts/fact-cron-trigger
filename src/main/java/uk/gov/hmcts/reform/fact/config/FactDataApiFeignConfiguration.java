package uk.gov.hmcts.reform.fact.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

/**
 * Feign OAuth2 setup for Fact Data API calls only.
 *
 * <p>This class creates a client-credentials authorized client manager and a request interceptor
 * that adds a bearer token to requests sent by the `factDataApi` Feign client.
 */
public class FactDataApiFeignConfiguration {

    /**
     * Builds a non-servlet OAuth2 client manager for client-credentials token acquisition.
     */
    @Bean("factDataApiAuthorizedClientManager")
    OAuth2AuthorizedClientManager factDataApiAuthorizedClientManager(
        ClientRegistrationRepository clientRegistrationRepository,
        OAuth2AuthorizedClientService authorizedClientService
    ) {
        AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
            new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                clientRegistrationRepository, authorizedClientService);
        OAuth2AuthorizedClientProvider provider = OAuth2AuthorizedClientProviderBuilder.builder()
            .clientCredentials()
            .build();
        manager.setAuthorizedClientProvider(provider);
        return manager;
    }

    /**
     * Adds an Authorization header to Feign requests for the configured Fact Data API client.
     */
    @Bean
    public RequestInterceptor bearerTokenRequestInterceptor(
        @Qualifier("factDataApiAuthorizedClientManager") OAuth2AuthorizedClientManager authorizedClientManager,
        @Value("${fact-data-api.oauth2.client-registration-id:fact-data-api}") String clientRegistrationId,
        @Value("${fact-data-api.feign-client-name:factDataApi}") String factDataApiFeignClientName
    ) {
        return requestTemplate -> {
            // Keep OAuth2 scoped to Fact Data API only.
            if (requestTemplate.feignTarget() == null
                || !factDataApiFeignClientName.equals(requestTemplate.feignTarget().name())) {
                return;
            }

            OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId(clientRegistrationId)
                .principal("fact-cron-trigger")
                .build();

            OAuth2AuthorizedClient client = authorizedClientManager.authorize(authorizeRequest);
            if (client == null || client.getAccessToken() == null) {
                throw new IllegalStateException("Failed to authorize OAuth2 client: " + clientRegistrationId);
            }

            requestTemplate.header("Authorization", "Bearer " + client.getAccessToken().getTokenValue());
        };
    }
}



