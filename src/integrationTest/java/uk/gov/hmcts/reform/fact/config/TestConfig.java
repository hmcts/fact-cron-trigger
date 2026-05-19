package uk.gov.hmcts.reform.fact.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestConfig {

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager() {
        return mock(OAuth2AuthorizedClientManager.class);
    }

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        return new InMemoryClientRegistrationRepository(
            ClientRegistration.withRegistrationId("factDataApi")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .clientId("mock-client")
                .tokenUri("http://mock-token-uri")
                .build()
        );
    }

    @Bean
    public OAuth2AuthorizedClientService authorizedClientService() {
        return mock(OAuth2AuthorizedClientService.class);
    }
}
