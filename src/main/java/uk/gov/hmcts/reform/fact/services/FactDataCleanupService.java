package uk.gov.hmcts.reform.fact.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.reform.fact.config.FactDataApiAdminUserProperties;
import uk.gov.hmcts.reform.fact.factapi.FactDataApiClient;

import java.util.Map;
import java.util.UUID;

@Service
public class FactDataCleanupService {

    private final FactDataApiClient factDataApiClient;
    private final FactDataApiAdminUserProperties adminUserProperties;

    public FactDataCleanupService(@Autowired FactDataApiClient factDataApiClient,
                                  @Autowired FactDataApiAdminUserProperties adminUserProperties) {
        this.factDataApiClient = factDataApiClient;
        this.adminUserProperties = adminUserProperties;
    }

    public void cleanupUsers() {
        factDataApiClient.deleteUsers();
    }

    public void cleanupAudits() {
        String ssoId = resolveAdminUserSsoId();
        Map<String, Object> userResponse = factDataApiClient.createOrUpdateUser(
            Map.of(
                "email", adminUserProperties.getEmail(),
                "role", adminUserProperties.getRole(),
                "ssoId", ssoId
            )
        );
        String userId = resolveUserId(userResponse);
        factDataApiClient.deleteAudits(userId);
    }

    private String resolveUserId(Map<String, Object> userResponse) {
        String userId = userResponse == null ? null : String.valueOf(userResponse.get("id"));
        if (!StringUtils.hasText(userId) || "null".equalsIgnoreCase(userId)) {
            throw new IllegalStateException("Missing 'id' in user create/update response");
        }
        return userId;
    }

    private String resolveAdminUserSsoId() {
        String ssoId = adminUserProperties.getSsoId();
        if (!StringUtils.hasText(ssoId)) {
            throw new IllegalStateException("fact-data-api.admin-user.sso-id must be set");
        }
        try {
            UUID.fromString(ssoId);
            return ssoId;
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("fact-data-api.admin-user.sso-id must be a UUID", ex);
        }
    }
}

