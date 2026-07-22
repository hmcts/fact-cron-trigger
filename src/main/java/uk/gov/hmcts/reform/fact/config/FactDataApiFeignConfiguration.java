package uk.gov.hmcts.reform.fact.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import uk.gov.hmcts.reform.fact.services.AzureAdTokenService;

public class FactDataApiFeignConfiguration {

    @Bean
    public RequestInterceptor bearerTokenRequestInterceptor(AzureAdTokenService azureAdTokenService) {
        return requestTemplate -> requestTemplate.header("Authorization",
                                                         "Bearer " + azureAdTokenService.getAccessToken());
    }
}

