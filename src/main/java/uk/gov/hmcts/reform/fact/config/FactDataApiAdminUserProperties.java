package uk.gov.hmcts.reform.fact.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "fact-data-api.admin-user")
@Getter
@Setter
public class FactDataApiAdminUserProperties {
    private String email;
    private String role;
    private String ssoId;
}

