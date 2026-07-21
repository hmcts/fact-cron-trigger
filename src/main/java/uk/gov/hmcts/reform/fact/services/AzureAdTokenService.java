package uk.gov.hmcts.reform.fact.services;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.reform.fact.config.AzureAdProperties;

import java.time.OffsetDateTime;

@Service
public class AzureAdTokenService {

    private final AzureAdProperties azureAdProperties;
    private final String scope;

    private volatile ClientSecretCredential credential;
    private volatile String cachedToken;
    private volatile OffsetDateTime cachedTokenExpiry;

    public AzureAdTokenService(AzureAdProperties azureAdProperties) {
        this.azureAdProperties = azureAdProperties;
        this.scope = resolveScope(azureAdProperties.getApplicationRegistrationId());
    }

    public synchronized String getAccessToken() {
        OffsetDateTime now = OffsetDateTime.now();
        if (cachedToken != null && cachedTokenExpiry != null && now.isBefore(cachedTokenExpiry.minusMinutes(2))) {
            return cachedToken;
        }

        if (!StringUtils.hasText(scope)) {
            throw new IllegalStateException("azure.ad.scope or azure.ad.application-registration-id is required for cleanup jobs");
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
        if (credential == null) {
            validateAuthProperties();
            credential = new ClientSecretCredentialBuilder()
              //  .tenantId(azureAdProperties.getTenantId())
                .clientId(azureAdProperties.getClientId())
                .clientSecret(azureAdProperties.getClientSecret())
                .build();
        }
        return credential;
    }

    private void validateAuthProperties() {
        if (
                //!StringUtils.hasText(azureAdProperties.getTenantId())
            //||
        !StringUtils.hasText(azureAdProperties.getClientId())
            || !StringUtils.hasText(azureAdProperties.getClientSecret())) {
            throw new IllegalStateException(
               // "azure.ad.tenant-id, azure.ad.client-id and azure.ad.client-secret are required for cleanup jobs"
                "azure.ad.client-id and azure.ad.client-secret are required for cleanup jobs"
            );
        }
    }

    private String resolveScope( String applicationRegistrationId) {
        if (StringUtils.hasText(applicationRegistrationId)) {
            return "api://" + applicationRegistrationId + "/.default";
        }
        return "";
    }
}
