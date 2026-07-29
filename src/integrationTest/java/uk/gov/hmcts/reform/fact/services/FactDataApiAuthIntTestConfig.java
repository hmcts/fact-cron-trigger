package uk.gov.hmcts.reform.fact.services;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import uk.gov.hmcts.reform.fact.config.FactDataApiFeignConfiguration;

@TestConfiguration
@EnableAutoConfiguration
@Import(FactDataApiFeignConfiguration.class)
class FactDataApiAuthIntTestConfig {
    // Minimal context for auth wiring checks.
}

