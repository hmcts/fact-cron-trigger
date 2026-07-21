package uk.gov.hmcts.reform.fact.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "azure.ad")
@Getter
@Setter
public class AzureAdProperties {

    //private String tenantId;
    private String clientId;
    private String clientSecret;
    private String applicationRegistrationId;
    //private String scope;

}
