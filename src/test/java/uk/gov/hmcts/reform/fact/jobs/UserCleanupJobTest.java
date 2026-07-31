package uk.gov.hmcts.reform.fact.jobs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.integrations.SlackMessageHelper;
import uk.gov.hmcts.reform.fact.integrations.SlackNotificationConstants;
import uk.gov.hmcts.reform.fact.services.FactDataCleanupService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserCleanupJobTest {

    @Mock
    private FactDataCleanupService cleanupService;

    @Mock
    private SlackMessageHelper slackMessageHelper;

    @Test
    void shouldSendSuccessSlackMessageOnCleanupSuccess() {
        UserCleanupJob job = new UserCleanupJob(cleanupService, slackMessageHelper);

        job.execute();

        verify(cleanupService).cleanupUsers();
        verify(slackMessageHelper).sendDailyCheckSummary(
            SlackNotificationConstants.USER_CLEANUP.getServiceName(),
            SlackNotificationConstants.USER_CLEANUP.getSuccessIcon(),
            Optional.empty()
        );
    }

    @Test
    void shouldSendFailureSlackMessageAndRethrowOnCleanupFailure() {
        UserCleanupJob job = new UserCleanupJob(cleanupService, slackMessageHelper);
        doThrow(new IllegalStateException("boom")).when(cleanupService).cleanupUsers();

        assertThatThrownBy(job::execute)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("boom");

        verify(slackMessageHelper).sendDailyCheckSummary(
            SlackNotificationConstants.USER_CLEANUP.getServiceName(),
            SlackNotificationConstants.USER_CLEANUP.getFailureIcon(),
            Optional.of(SlackNotificationConstants.USER_CLEANUP.getFailurePrefix() + "boom")
        );
    }
}


