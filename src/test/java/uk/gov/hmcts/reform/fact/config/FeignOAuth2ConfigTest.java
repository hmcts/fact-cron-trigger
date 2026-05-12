package uk.gov.hmcts.reform.fact.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeignOAuth2ConfigTest {

    @Mock
    private OAuth2AuthorizedClientManager authorizedClientManager;

    @InjectMocks
    private FeignOAuth2Config feignOAuth2Config;

    @Test
    void shouldAddAuthorizationHeaderWhenTokenIsAvailable() {
        RequestInterceptor interceptor = feignOAuth2Config.requestInterceptor(authorizedClientManager);
        RequestTemplate template = new RequestTemplate();

        OAuth2AuthorizedClient authorizedClient = mock(OAuth2AuthorizedClient.class);
        OAuth2AccessToken accessToken = mock(OAuth2AccessToken.class);
        when(accessToken.getTokenValue()).thenReturn("test-token");
        when(authorizedClient.getAccessToken()).thenReturn(accessToken);

        ArgumentCaptor<OAuth2AuthorizeRequest> captor = ArgumentCaptor.forClass(OAuth2AuthorizeRequest.class);
        when(authorizedClientManager.authorize(captor.capture())).thenReturn(authorizedClient);

        interceptor.apply(template);

        assertThat(template.headers().get("Authorization")).containsExactly("Bearer test-token");
        assertThat(captor.getValue().getClientRegistrationId()).isEqualTo("factDataApi");
        assertThat(captor.getValue().getPrincipal().toString()).contains("fact-cron-trigger");
    }

    @Test
    void shouldNotAddAuthorizationHeaderWhenAuthorizedClientIsNull() {
        RequestInterceptor interceptor = feignOAuth2Config.requestInterceptor(authorizedClientManager);
        RequestTemplate template = new RequestTemplate();

        when(authorizedClientManager.authorize(any())).thenReturn(null);

        interceptor.apply(template);

        assertThat(template.headers().get("Authorization")).isNull();
    }

    @Test
    void shouldNotAddAuthorizationHeaderWhenAccessTokenIsNull() {
        RequestInterceptor interceptor = feignOAuth2Config.requestInterceptor(authorizedClientManager);
        RequestTemplate template = new RequestTemplate();

        OAuth2AuthorizedClient authorizedClient = mock(OAuth2AuthorizedClient.class);
        when(authorizedClient.getAccessToken()).thenReturn(null);
        when(authorizedClientManager.authorize(any())).thenReturn(authorizedClient);

        interceptor.apply(template);

        assertThat(template.headers().get("Authorization")).isNull();
    }
}
