package uk.gov.hmcts.reform.fact.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.Target;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeignFactDataConfigTest {

    @Mock
    private OAuth2AuthorizedClientManager authorizedClientManager;

    @Mock
    private ClientRegistrationRepository clientRegistrationRepository;

    @Mock
    private OAuth2AuthorizedClientService authorizedClientService;

    @InjectMocks
    private FeignFactDataConfig feignOAuth2Config;

    @Test
    void shouldAddAuthorizationHeaderWhenTokenIsAvailable() {
        OAuth2AuthorizedClient authorizedClient = mock(OAuth2AuthorizedClient.class);
        OAuth2AccessToken accessToken = mock(OAuth2AccessToken.class);
        when(accessToken.getTokenValue()).thenReturn("test-token");
        when(authorizedClient.getAccessToken()).thenReturn(accessToken);

        ArgumentCaptor<OAuth2AuthorizeRequest> captor = ArgumentCaptor.forClass(OAuth2AuthorizeRequest.class);
        when(authorizedClientManager.authorize(captor.capture())).thenReturn(authorizedClient);

        RequestTemplate template = new RequestTemplate();
        template.feignTarget(new Target.HardCodedTarget<>(String.class, "factDataApi", "http://localhost"));
        feignOAuth2Config.requestInterceptor(authorizedClientManager).apply(template);

        assertThat(template.headers().get("Authorization")).containsExactly("Bearer test-token");
        assertThat(captor.getValue().getClientRegistrationId()).isEqualTo("factDataApi");
        assertThat(captor.getValue().getPrincipal().toString()).contains("fact-cron-trigger");
    }

    @Test
    void shouldNotAddAuthorizationHeaderWhenAuthorizedClientIsNull() {
        RequestInterceptor interceptor = feignOAuth2Config.requestInterceptor(authorizedClientManager);
        RequestTemplate template = new RequestTemplate();
        template.feignTarget(new Target.HardCodedTarget<>(String.class, "factDataApi", "http://localhost"));

        when(authorizedClientManager.authorize(any())).thenReturn(null);

        interceptor.apply(template);

        assertThat(template.headers().get("Authorization")).isNull();
    }

    @Test
    void shouldNotAddAuthorizationHeaderWhenAccessTokenIsNull() {
        RequestTemplate template = new RequestTemplate();
        template.feignTarget(new Target.HardCodedTarget<>(String.class, "factDataApi", "http://localhost"));

        OAuth2AuthorizedClient authorizedClient = mock(OAuth2AuthorizedClient.class);
        when(authorizedClient.getAccessToken()).thenReturn(null);
        when(authorizedClientManager.authorize(any())).thenReturn(authorizedClient);

        RequestInterceptor interceptor = feignOAuth2Config.requestInterceptor(authorizedClientManager);
        interceptor.apply(template);

        assertThat(template.headers().get("Authorization")).isNull();
    }

    @Test
    void shouldReturnAuthorizedClientManager() {
        OAuth2AuthorizedClientManager manager = feignOAuth2Config.authorizedClientManager(
            clientRegistrationRepository,
            authorizedClientService
        );

        assertThat(manager).isNotNull();
        assertThat(manager).isInstanceOf(AuthorizedClientServiceOAuth2AuthorizedClientManager.class);
    }
}
