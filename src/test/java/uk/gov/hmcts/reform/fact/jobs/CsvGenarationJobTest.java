package uk.gov.hmcts.reform.fact.jobs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.integrations.SlackMessageHelper;
import uk.gov.hmcts.reform.fact.integrations.SlackNotificationConstants;
import uk.gov.hmcts.reform.fact.services.FactDataService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CsvGenarationJobTest {

    @Mock
    private FactDataService factDataService;

    @Mock
    private SlackMessageHelper slackMessageHelper;

    @Test
    void shouldBeApplicableOnlyForCsvGenerationNewTrigger() {
        CsvGenarationJob job = new CsvGenarationJob(factDataService, slackMessageHelper);

        assertThat(job.isApplicable(ScheduleTypes.CSV_GENERATION_NEW)).isTrue();
        assertThat(job.isApplicable(ScheduleTypes.CSV)).isFalse();
    }

    @Test
    void shouldSendSuccessSlackMessageOnCsvGenerationSuccess() {
        CsvGenarationJob job = new CsvGenarationJob(factDataService, slackMessageHelper);

        job.execute();

        verify(factDataService).generateCSV();
        verify(slackMessageHelper).sendDailyCheckSummary(
            SlackNotificationConstants.CSV_GENERATION.getServiceName(),
            SlackNotificationConstants.CSV_GENERATION.getSuccessIcon(),
            Optional.empty()
        );
    }

    @Test
    void shouldSendFailureSlackMessageAndRethrowOnCsvGenerationFailure() {
        CsvGenarationJob job = new CsvGenarationJob(factDataService, slackMessageHelper);
        doThrow(new IllegalStateException("boom")).when(factDataService).generateCSV();

        assertThatThrownBy(job::execute)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("boom");

        verify(slackMessageHelper).sendDailyCheckSummary(
            SlackNotificationConstants.CSV_GENERATION.getServiceName(),
            SlackNotificationConstants.CSV_GENERATION.getFailureIcon(),
            Optional.of(
                SlackNotificationConstants.CSV_GENERATION.getFailurePrefix()
                    + SlackNotificationConstants.FAILURE_DETAILS_SUFFIX
            )
        );
    }
}
