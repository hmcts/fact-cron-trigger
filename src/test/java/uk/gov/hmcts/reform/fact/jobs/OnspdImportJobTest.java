package uk.gov.hmcts.reform.fact.jobs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.factapi.FactDataApiClient;
import uk.gov.hmcts.reform.fact.integrations.SlackMessageHelper;
import uk.gov.hmcts.reform.fact.integrations.SlackNotificationConstants;
import uk.gov.hmcts.reform.fact.models.PostcodeReferenceSynchronisationResult;

import java.time.Instant;
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
    private FactDataApiClient factDataApiClient;

    @Mock
    private SlackMessageHelper slackMessageHelper;

    @Test
    void appliesOnlyToOnspdImportSchedule() {
        OnspdImportJob job = new OnspdImportJob(factDataApiClient, slackMessageHelper);

        assertThat(job.isApplicable(ScheduleTypes.ONSPD_IMPORT)).isTrue();
        assertThat(job.isApplicable(ScheduleTypes.CSV)).isFalse();
    }

    @Test
    void notifiesSlackAfterSynchronisingRelease() {
        when(factDataApiClient.synchronisePostcodeReference()).thenReturn(result(true, true));
        OnspdImportJob job = new OnspdImportJob(factDataApiClient, slackMessageHelper);

        job.execute();

        verify(slackMessageHelper).sendLongMessage(contains("993,979"));
        verify(slackMessageHelper).sendLongMessage(contains("onspd/postcode-reference.csv"));
    }

    @Test
    void notifiesSlackWhenExistingBlobWasImported() {
        when(factDataApiClient.synchronisePostcodeReference()).thenReturn(result(false, true));
        OnspdImportJob job = new OnspdImportJob(factDataApiClient, slackMessageHelper);

        job.execute();

        verify(slackMessageHelper).sendLongMessage(contains("2026-08"));
    }

    @Test
    void doesNotNotifySlackWhenReleaseAndDatabaseAreCurrent() {
        when(factDataApiClient.synchronisePostcodeReference()).thenReturn(result(false, false));
        OnspdImportJob job = new OnspdImportJob(factDataApiClient, slackMessageHelper);

        job.execute();

        verify(slackMessageHelper, never()).sendLongMessage(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void notifiesSlackAndRethrowsWhenSynchronisationFails() {
        doThrow(new IllegalStateException("boom")).when(factDataApiClient).synchronisePostcodeReference();
        OnspdImportJob job = new OnspdImportJob(factDataApiClient, slackMessageHelper);

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

    private PostcodeReferenceSynchronisationResult result(boolean blobUpdated, boolean imported) {
        return new PostcodeReferenceSynchronisationResult(
            blobUpdated,
            imported,
            "aug:3",
            "2026-08",
            993_979,
            "onspd/postcode-reference.csv",
            Instant.parse("2026-10-20T09:00:00Z")
        );
    }
}
