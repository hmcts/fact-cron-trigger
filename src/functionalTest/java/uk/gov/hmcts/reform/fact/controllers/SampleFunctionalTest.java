package uk.gov.hmcts.reform.fact.controllers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class SampleFunctionalTest {

    /**
     * Base test for now for the pipeline. Although it is disabled currently/not run
     * as the cron job is marked as a nonserviceapp().
     * Entire end to end can be found in the integration tests.
     */
    @Test
    void functionalTest() {
        Assertions.assertTrue(true);
    }
}
