package uk.gov.hmcts.reform.fact.jobs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.integrations.SlackMessageHelper;
import uk.gov.hmcts.reform.fact.integrations.SlackNotificationConstants;
import uk.gov.hmcts.reform.fact.services.FactDataService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditCleanupJobTest {

    @Mock
    private FactDataService cleanupService;

    @Mock
    private SlackMessageHelper slackMessageHelper;

    @Test
    void shouldSendSuccessSlackMessageOnCleanupSuccess() {
        AuditCleanupJob job = new AuditCleanupJob(cleanupService, slackMessageHelper);

        job.execute();

        verify(cleanupService).cleanupAudits();
        verify(slackMessageHelper).sendDailyCheckSummary(
            SlackNotificationConstants.AUDIT_CLEANUP.getServiceName(),
            SlackNotificationConstants.AUDIT_CLEANUP.getSuccessIcon(),
            Optional.empty()
        );
    }

    @Test
    void shouldSendFailureSlackMessageAndRethrowOnCleanupFailure() {
        AuditCleanupJob job = new AuditCleanupJob(cleanupService, slackMessageHelper);
        doThrow(new IllegalStateException("boom")).when(cleanupService).cleanupAudits();

        assertThatThrownBy(job::execute)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("boom");

        verify(slackMessageHelper).sendDailyCheckSummary(
            SlackNotificationConstants.AUDIT_CLEANUP.getServiceName(),
            SlackNotificationConstants.AUDIT_CLEANUP.getFailureIcon(),
            Optional.of(
                SlackNotificationConstants.AUDIT_CLEANUP.getFailurePrefix()
                    + SlackNotificationConstants.FAILURE_DETAILS_SUFFIX
            )
        );
    }
}
