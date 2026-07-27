package uk.gov.hmcts.reform.fact.integrations;

import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.reform.fact.config.SlackProperties;

import java.io.IOException;

@Slf4j
@Component
public class SlackClient {

    private final SlackProperties properties;

    public SlackClient(SlackProperties properties) {
        this.properties = properties;
    }

    public void sendSlackMessage(String message) {
        if (!StringUtils.hasText(properties.getTokenDailyChecks())
            || !StringUtils.hasText(properties.getChannelIdDailyChecks())) {
            log.warn("Slack token/channel is not configured; skipping Slack message");
            return;
        }

        try {
            ChatPostMessageResponse response = Slack.getInstance()
                .methods(properties.getTokenDailyChecks())
                .chatPostMessage(r -> r
                    .channel(properties.getChannelIdDailyChecks())
                    .text(message)
                );

            if (!response.isOk()) {
                throw new IllegalStateException("Slack API error: " + response.getError());
            }
        } catch (IOException | SlackApiException ex) {
            log.error("Exception occurred while calling Slack API: {}", ex.getMessage());
        }
    }
}

