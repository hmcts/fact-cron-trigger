package uk.gov.hmcts.reform.fact.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;

public class FeignFactConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            return;
        };
    }
}
