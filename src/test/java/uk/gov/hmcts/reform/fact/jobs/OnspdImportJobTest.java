package uk.gov.hmcts.reform.fact.jobs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.integrations.SlackMessageHelper;
import uk.gov.hmcts.reform.fact.integrations.SlackNotificationConstants;
import uk.gov.hmcts.reform.fact.models.OnspdUpdateResult;
import uk.gov.hmcts.reform.fact.services.OnspdService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnspdImportJobTest {

    @Mock
    private OnspdService onspdService;

    @Mock
    private SlackMessageHelper slackMessageHelper;

    @Test
    void shouldApplyToOnspdImportSchedule() {
        OnspdImportJob job = new OnspdImportJob(onspdService, slackMessageHelper);

        assertThat(job.isApplicable(ScheduleTypes.ONSPD_IMPORT)).isTrue();
        assertThat(job.isApplicable(ScheduleTypes.CSV)).isFalse();
    }

    @Test
    void shouldNotifySlackAfterUploadingNewRelease() {
        when(onspdService.updateIfRequired()).thenReturn(
            OnspdUpdateResult.updated("2026-08", 993_979, "onspd/postcode-reference.csv")
        );
        OnspdImportJob job = new OnspdImportJob(onspdService, slackMessageHelper);

        job.execute();

        verify(slackMessageHelper).sendLongMessage(contains("993,979"));
        verify(slackMessageHelper).sendLongMessage(contains("onspd/postcode-reference.csv"));
    }

    @Test
    void shouldNotNotifySlackWhenReleaseIsUnchanged() {
        when(onspdService.updateIfRequired()).thenReturn(
            OnspdUpdateResult.unchanged("2026-08", "onspd/postcode-reference.csv")
        );
        OnspdImportJob job = new OnspdImportJob(onspdService, slackMessageHelper);

        job.execute();

        verify(slackMessageHelper, never()).sendLongMessage(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void shouldNotifySlackAndRethrowWhenImportFails() {
        doThrow(new IllegalStateException("boom")).when(onspdService).updateIfRequired();
        OnspdImportJob job = new OnspdImportJob(onspdService, slackMessageHelper);

        assertThatThrownBy(job::execute)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("boom");

        verify(slackMessageHelper).sendDailyCheckSummary(
            SlackNotificationConstants.ONSPD_IMPORT.getServiceName(),
            SlackNotificationConstants.ONSPD_IMPORT.getFailureIcon(),
            Optional.of(
                SlackNotificationConstants.ONSPD_IMPORT.getFailurePrefix()
                    + SlackNotificationConstants.FAILURE_DETAILS_SUFFIX
            )
        );
    }
}
