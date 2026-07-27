package uk.gov.hmcts.reform.fact.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.config.FactDataApiAdminUserProperties;
import uk.gov.hmcts.reform.fact.factapi.FactDataApiClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FactDataCleanupServiceTest {

    @Mock
    private FactDataApiClient factDataApiClient;

    private FactDataApiAdminUserProperties adminUserProperties(String ssoId) {
        FactDataApiAdminUserProperties properties = new FactDataApiAdminUserProperties();
        properties.setEmail("admin@example.net");
        properties.setRole("FACT_ADMIN");
        properties.setSsoId(ssoId);
        return properties;
    }

    @Test
    void shouldCreateUserAndPassXUserIdBeforeAuditCleanup() {
        FactDataCleanupService service = new FactDataCleanupService(
            factDataApiClient,
            adminUserProperties("11111111-1111-1111-1111-111111111111")
        );

        when(factDataApiClient.createOrUpdateUser(anyMap())).thenReturn(Map.of("id", "12345"));

        service.cleanupAudits();

        verify(factDataApiClient).createOrUpdateUser(
            Map.of(
                "email", "admin@example.net",
                "role", "FACT_ADMIN",
                "ssoId", "11111111-1111-1111-1111-111111111111"
            )
        );
        verify(factDataApiClient).deleteAudits("12345");

        InOrder inOrder = inOrder(factDataApiClient);
        inOrder.verify(factDataApiClient).createOrUpdateUser(anyMap());
        inOrder.verify(factDataApiClient).deleteAudits("12345");
    }

    @Test
    void shouldFailWhenCreateUserResponseDoesNotContainId() {
        FactDataCleanupService service = new FactDataCleanupService(
            factDataApiClient,
            adminUserProperties("11111111-1111-1111-1111-111111111111")
        );

        when(factDataApiClient.createOrUpdateUser(anyMap())).thenReturn(Map.of("status", "ok"));

        assertThatThrownBy(service::cleanupAudits)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Missing 'id'");
    }

    @Test
    void shouldFailWhenSsoIdIsNotUuid() {
        FactDataCleanupService service = new FactDataCleanupService(
            factDataApiClient,
            adminUserProperties("not-a-uuid")
        );

        assertThatThrownBy(service::cleanupAudits)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("must be a UUID");
    }
}




