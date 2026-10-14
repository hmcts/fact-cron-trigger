package uk.gov.hmcts.reform.fact.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "onspd")
@Getter
@Setter
public class OnspdProperties {

    private String catalogueUrl;
    private String allowedServiceHost;
    private String containerName;
    private String blobName;
    private int pageSize = 1000;
    private int maxAttempts = 4;
    private long initialRetryBackoffMillis = 1000;
}
