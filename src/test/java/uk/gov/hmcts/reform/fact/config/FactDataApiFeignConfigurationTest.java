package uk.gov.hmcts.reform.fact.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.Target;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FactDataApiFeignConfigurationTest {

    @Mock
    private OAuth2AuthorizedClientManager authorizedClientManager;

    @Mock
    private ClientRegistrationRepository clientRegistrationRepository;

    @Mock
    private OAuth2AuthorizedClientService authorizedClientService;

    @Test
    void shouldCreateAuthorizedClientManagerBean() {
        FactDataApiFeignConfiguration configuration = new FactDataApiFeignConfiguration();

        OAuth2AuthorizedClientManager manager = configuration.factDataApiAuthorizedClientManager(
            clientRegistrationRepository,
            authorizedClientService
        );

        assertThat(manager).isNotNull();
    }

    @Test
    void shouldSkipWhenFeignTargetIsMissing() {
        FactDataApiFeignConfiguration configuration = new FactDataApiFeignConfiguration();
        RequestInterceptor interceptor = configuration.bearerTokenRequestInterceptor(
            authorizedClientManager,
            "fact-data-api",
            "factDataApi"
        );
        RequestTemplate requestTemplate = new RequestTemplate();

        interceptor.apply(requestTemplate);

        verifyNoInteractions(authorizedClientManager);
        assertThat(requestTemplate.headers()).doesNotContainKey("Authorization");
    }

    @Test
    void shouldSkipWhenFeignTargetNameDoesNotMatch() {
        FactDataApiFeignConfiguration configuration = new FactDataApiFeignConfiguration();
        RequestInterceptor interceptor = configuration.bearerTokenRequestInterceptor(
            authorizedClientManager,
            "fact-data-api",
            "factDataApi"
        );
        RequestTemplate requestTemplate = new RequestTemplate();
        requestTemplate.feignTarget(new Target.HardCodedTarget<>(Object.class, "otherClient", "http://localhost"));

        interceptor.apply(requestTemplate);

        verifyNoInteractions(authorizedClientManager);
        assertThat(requestTemplate.headers()).doesNotContainKey("Authorization");
    }

    @Test
    void shouldAddBearerHeaderForMatchingFeignTarget() {
        FactDataApiFeignConfiguration configuration = new FactDataApiFeignConfiguration();
        RequestInterceptor interceptor = configuration.bearerTokenRequestInterceptor(
            authorizedClientManager,
            "fact-data-api",
            "factDataApi"
        );
        RequestTemplate requestTemplate = new RequestTemplate();
        requestTemplate.feignTarget(new Target.HardCodedTarget<>(Object.class, "factDataApi", "http://localhost"));
        when(authorizedClientManager.authorize(any())).thenReturn(authorizedClientWithToken("token-123"));

        interceptor.apply(requestTemplate);

        ArgumentCaptor<OAuth2AuthorizeRequest> captor = ArgumentCaptor.forClass(OAuth2AuthorizeRequest.class);
        verify(authorizedClientManager).authorize(captor.capture());
        assertThat(captor.getValue().getClientRegistrationId()).isEqualTo("fact-data-api");

        Collection<String> authHeaders = requestTemplate.headers()
            .getOrDefault("Authorization", Collections.emptyList());
        assertThat(authHeaders).containsExactly("Bearer token-123");
    }

    @Test
    void shouldThrowWhenAuthorizationFails() {
        FactDataApiFeignConfiguration configuration = new FactDataApiFeignConfiguration();
        RequestInterceptor interceptor = configuration.bearerTokenRequestInterceptor(
            authorizedClientManager,
            "fact-data-api",
            "factDataApi"
        );
        RequestTemplate requestTemplate = new RequestTemplate();
        requestTemplate.feignTarget(new Target.HardCodedTarget<>(Object.class, "factDataApi", "http://localhost"));
        when(authorizedClientManager.authorize(any())).thenReturn(null);

        assertThatThrownBy(() -> interceptor.apply(requestTemplate))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Failed to authorize OAuth2 client: fact-data-api");
    }

    private OAuth2AuthorizedClient authorizedClientWithToken(String tokenValue) {
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            tokenValue,
            Instant.now(),
            Instant.now().plusSeconds(300)
        );
        return new OAuth2AuthorizedClient(clientRegistration(), "fact-cron-trigger", accessToken);
    }

    private ClientRegistration clientRegistration() {
        return ClientRegistration.withRegistrationId("fact-data-api")
            .tokenUri("https://example.com/oauth2/token")
            .clientId("client-id")
            .clientSecret("client-secret")
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .build();
    }
}



