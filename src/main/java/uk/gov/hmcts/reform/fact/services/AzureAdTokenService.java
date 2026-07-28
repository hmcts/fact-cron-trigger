package uk.gov.hmcts.reform.fact.services;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.reform.fact.config.AzureAdProperties;

import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class AzureAdTokenService {

    private final AzureAdProperties azureAdProperties;
    private final String scope;

    private final AtomicReference<ClientSecretCredential> credential = new AtomicReference<>();
    private volatile String cachedToken;
    private volatile OffsetDateTime cachedTokenExpiry;

    public AzureAdTokenService(AzureAdProperties azureAdProperties) {
        this.azureAdProperties = azureAdProperties;
        this.scope = azureAdProperties.getScope();
    }

    public synchronized String getAccessToken() {
        OffsetDateTime now = OffsetDateTime.now();
        if (cachedToken != null && cachedTokenExpiry != null && now.isBefore(cachedTokenExpiry.minusMinutes(2))) {
            return cachedToken;
        }

        if (!StringUtils.hasText(scope) || "api:///.default".equals(scope)) {
            throw new IllegalStateException(
                "azure.ad.scope is required for cleanup jobs");
        }

        AccessToken token = getCredential().getToken(new TokenRequestContext().addScopes(scope)).block();
        if (token == null) {
            throw new IllegalStateException("Failed to obtain Azure AD access token");
        }

        cachedToken = token.getToken();
        cachedTokenExpiry = token.getExpiresAt();
        return cachedToken;
    }

    private ClientSecretCredential getCredential() {
        ClientSecretCredential existing = credential.get();
        if (existing != null) {
            return existing;
        }

        validateAuthProperties();
        ClientSecretCredential created = new ClientSecretCredentialBuilder()
            .tenantId(azureAdProperties.getTenantId())
            .clientId(azureAdProperties.getClientId())
            .clientSecret(azureAdProperties.getClientSecret())
            .build();

        if (credential.compareAndSet(null, created)) {
            return created;
        }

        return credential.get();
    }

    private void validateAuthProperties() {
        if (
            !StringUtils.hasText(azureAdProperties.getTenantId())
                || !StringUtils.hasText(azureAdProperties.getClientId())
                || !StringUtils.hasText(azureAdProperties.getClientSecret())) {
            throw new IllegalStateException(
                 "azure.ad.tenant-id, azure.ad.client-id and azure.ad.client-secret are required for cleanup jobs"
            );
        }
    }

}
