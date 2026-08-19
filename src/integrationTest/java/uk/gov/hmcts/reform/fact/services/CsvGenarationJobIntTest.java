package uk.gov.hmcts.reform.fact.services;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.reform.fact.factapi.FactDataApiClient;
import uk.gov.hmcts.reform.fact.integrations.SlackMessageHelper;
import uk.gov.hmcts.reform.fact.integrations.SlackNotificationConstants;

import java.util.Optional;

import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {
    "app.trigger-type=CSV_GENERATION_NEW",
    "app.enabled=true"
})
class CsvGenarationJobIntTest {

    @MockitoBean
    private FactDataApiClient factDataApiClient;

    @MockitoBean
    private SlackMessageHelper slackMessageHelper;

    @Test
    void shouldExecuteCsvGenerationNewJobOnStartup() {
        verify(factDataApiClient).createAndUploadCsv();
        verify(slackMessageHelper).sendDailyCheckSummary(
            SlackNotificationConstants.CSV_GENERATION.getServiceName(),
            SlackNotificationConstants.CSV_GENERATION.getSuccessIcon(),
            Optional.empty()
        );
    }
}

