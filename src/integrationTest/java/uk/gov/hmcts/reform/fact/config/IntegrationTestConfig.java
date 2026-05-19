package uk.gov.hmcts.reform.fact.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import uk.gov.hmcts.reform.fact.factapi.FactClient;
import uk.gov.hmcts.reform.fact.services.AzureService;
import uk.gov.hmcts.reform.fact.services.FactService;

@Configuration
@EnableAutoConfiguration
@EnableFeignClients(clients = FactClient.class)
@Import({AzureBlobConfiguration.class, AzureService.class, FactService.class})
public class IntegrationTestConfig {
    // Focus this integration test on FACT API + blob upload wiring only.
}
