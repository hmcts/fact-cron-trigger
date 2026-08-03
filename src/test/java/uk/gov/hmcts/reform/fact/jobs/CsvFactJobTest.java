package uk.gov.hmcts.reform.fact.jobs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.integrations.SlackMessageHelper;
import uk.gov.hmcts.reform.fact.integrations.SlackNotificationConstants;
import uk.gov.hmcts.reform.fact.services.CsvGenerationService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CsvFactJobTest {

    @Mock
    private CsvGenerationService csvGenerationService;

    @Mock
    private SlackMessageHelper slackMessageHelper;

    @Test
    void shouldSendSuccessSlackMessageOnCsvGenerationSuccess() {
        CsvFactJob job = new CsvFactJob(csvGenerationService, slackMessageHelper);

        job.execute();

        verify(csvGenerationService).createCsvAndUpload();
        verify(slackMessageHelper).sendDailyCheckSummary(
            SlackNotificationConstants.CSV.getServiceName(),
            SlackNotificationConstants.CSV.getSuccessIcon(),
            Optional.empty()
        );
    }

    @Test
    void shouldSendFailureSlackMessageAndRethrowOnCsvGenerationFailure() {
        CsvFactJob job = new CsvFactJob(csvGenerationService, slackMessageHelper);
        doThrow(new IllegalStateException("boom")).when(csvGenerationService).createCsvAndUpload();

        assertThatThrownBy(job::execute)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("boom");

        verify(slackMessageHelper).sendDailyCheckSummary(
            SlackNotificationConstants.CSV.getServiceName(),
            SlackNotificationConstants.CSV.getFailureIcon(),
            Optional.of(
                SlackNotificationConstants.CSV.getFailurePrefix()
                    + SlackNotificationConstants.FAILURE_DETAILS_SUFFIX
            )
        );
    }
}
