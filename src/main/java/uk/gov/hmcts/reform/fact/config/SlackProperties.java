package uk.gov.hmcts.reform.fact.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "slack")
@Getter
@Setter
public class SlackProperties {
    private String tokenDailyChecks;
    private String channelIdDailyChecks;
}

