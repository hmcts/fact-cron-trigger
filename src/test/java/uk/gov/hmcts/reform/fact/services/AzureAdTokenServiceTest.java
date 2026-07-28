package uk.gov.hmcts.reform.fact.services;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import uk.gov.hmcts.reform.fact.config.AzureAdProperties;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AzureAdTokenServiceTest {

    private static final String VALID_SCOPE = "api://fact-cleanup/.default";

    @Mock
    private ClientSecretCredential credential;

    private AzureAdProperties azureAdProperties;
    private AzureAdTokenService service;

    @BeforeEach
    void setUp() {
        azureAdProperties = new AzureAdProperties();
        azureAdProperties.setScope(VALID_SCOPE);
        azureAdProperties.setTenantId("tenant-id");
        azureAdProperties.setClientId("client-id");
        azureAdProperties.setClientSecret("client-secret");
        service = new AzureAdTokenService(azureAdProperties);
    }

    @Test
    void shouldReturnCachedTokenWhenNotNearExpiry() throws Exception {
        setField(service, "cachedToken", "cached-token");
        setField(service, "cachedTokenExpiry", OffsetDateTime.now().plusMinutes(10));

        String token = service.getAccessToken();

        assertThat(token).isEqualTo("cached-token");
        verifyNoInteractions(credential);
    }

    @Test
    void shouldThrowWhenScopeIsBlank() {
        azureAdProperties.setScope(" ");
        AzureAdTokenService serviceWithBlankScope = new AzureAdTokenService(azureAdProperties);

        assertThatThrownBy(serviceWithBlankScope::getAccessToken)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("azure.ad.scope is required for cleanup jobs");
    }

    @Test
    void shouldThrowWhenScopeIsDefaultPlaceholder() {
        azureAdProperties.setScope("api:///.default");
        AzureAdTokenService serviceWithDefaultScope = new AzureAdTokenService(azureAdProperties);

        assertThatThrownBy(serviceWithDefaultScope::getAccessToken)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("azure.ad.scope is required for cleanup jobs");
    }

    @Test
    void shouldThrowWhenCredentialPropertiesAreMissing() {
        azureAdProperties.setClientSecret("");

        assertThatThrownBy(service::getAccessToken)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("azure.ad.tenant-id, azure.ad.client-id and "
                            + "azure.ad.client-secret are required for cleanup jobs");
    }

    @Test
    void shouldThrowWhenTokenAcquisitionReturnsNull() throws Exception {
        setField(service, "credential", new AtomicReference<>(credential));
        when(credential.getToken(any(TokenRequestContext.class))).thenReturn(Mono.empty());

        assertThatThrownBy(service::getAccessToken)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Failed to obtain Azure AD access token");
    }

    @Test
    void shouldAcquireAndCacheToken() throws Exception {
        AccessToken accessToken = new AccessToken("fresh-token", OffsetDateTime.now().plusMinutes(30));
        setField(service, "credential", new AtomicReference<>(credential));
        when(credential.getToken(any(TokenRequestContext.class))).thenReturn(Mono.just(accessToken));

        String firstToken = service.getAccessToken();
        String secondToken = service.getAccessToken();

        assertThat(firstToken).isEqualTo("fresh-token");
        assertThat(secondToken).isEqualTo("fresh-token");
        verify(credential).getToken(any(TokenRequestContext.class));
    }

    @Test
    void shouldBuildCredentialWhenMissingAndThenAcquireToken() {
        AccessToken accessToken = new AccessToken("built-token", OffsetDateTime.now().plusMinutes(30));

        try (MockedConstruction<ClientSecretCredentialBuilder> ignored = mockConstruction(
            ClientSecretCredentialBuilder.class,
            (builder, context) -> {
                when(builder.tenantId(anyString())).thenReturn(builder);
                when(builder.clientId(anyString())).thenReturn(builder);
                when(builder.clientSecret(anyString())).thenReturn(builder);
                when(builder.build()).thenReturn(credential);
            }
        )) {
            when(credential.getToken(any(TokenRequestContext.class))).thenReturn(Mono.just(accessToken));

            String token = service.getAccessToken();

            assertThat(token).isEqualTo("built-token");
            verify(credential).getToken(any(TokenRequestContext.class));
        }
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}




