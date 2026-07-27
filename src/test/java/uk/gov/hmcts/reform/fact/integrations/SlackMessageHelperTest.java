package uk.gov.hmcts.reform.fact.integrations;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SlackMessageHelperTest {

    @Mock
    private SlackClient slackClient;

    @Test
    void shouldSendWarningWhenMessageExceedsTotalLimit() {
        SlackMessageHelper helper = new SlackMessageHelper(slackClient);
        String oversized = "x".repeat(SlackMessageHelper.MAX_TOTAL + 1);

        helper.sendLongMessage(oversized);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(slackClient).sendSlackMessage(captor.capture());
        assertThat(captor.getValue()).contains("Message is too large to send");
    }

    @Test
    void shouldSplitLongMessageInto4kChunks() {
        SlackMessageHelper helper = new SlackMessageHelper(slackClient);
        String longMessage = "a".repeat(9001);

        helper.sendLongMessage(longMessage);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(slackClient, times(3)).sendSlackMessage(captor.capture());
        List<String> chunks = captor.getAllValues();
        assertThat(chunks.get(0)).hasSize(4000);
        assertThat(chunks.get(1)).hasSize(4000);
        assertThat(chunks.get(2)).hasSize(1001);
    }

    @Test
    void shouldSendAllClearDailySummaryWhenNoActions() {
        SlackMessageHelper helper = new SlackMessageHelper(slackClient);

        helper.sendDailyCheckSummary("FACT", ":white_check_mark:", Optional.empty());

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(slackClient).sendSlackMessage(captor.capture());
        assertThat(captor.getValue()).contains("FACT Daily Check");
        assertThat(captor.getValue()).contains("All clear");
    }

    @Test
    void shouldSendIssueDailySummaryWhenActionsExist() {
        SlackMessageHelper helper = new SlackMessageHelper(slackClient);

        helper.sendDailyCheckSummary("FACT", ":warning:", List.of("Rotate secret", "Re-run job"));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(slackClient).sendSlackMessage(captor.capture());
        assertThat(captor.getValue()).contains("FACT issue found");
        assertThat(captor.getValue()).contains("• Rotate secret");
        assertThat(captor.getValue()).contains("• Re-run job");
    }
}

