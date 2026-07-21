package uk.gov.hmcts.reform.fact.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.fact.services.AzureAdTokenService;

@Configuration
public class FactDataApiFeignConfiguration {

    @Bean
    public RequestInterceptor bearerTokenRequestInterceptor(AzureAdTokenService azureAdTokenService) {
        return requestTemplate -> requestTemplate.header("Authorization", "Bearer " + azureAdTokenService.getAccessToken());
    }
}

