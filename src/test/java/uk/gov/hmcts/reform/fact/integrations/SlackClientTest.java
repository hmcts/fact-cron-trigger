package uk.gov.hmcts.reform.fact.integrations;

import com.slack.api.Slack;
import com.slack.api.RequestConfigurator;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.config.SlackProperties;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlackClientTest {

    private static final String TOKEN = "xoxb-token";
    private static final String CHANNEL = "C123";

    @Mock
    private Slack slack;

    @Mock
    private MethodsClient methodsClient;

    @Mock
    private ChatPostMessageResponse response;

    private SlackProperties properties;
    private SlackClient slackClient;

    @BeforeEach
    void setUp() {
        properties = new SlackProperties();
        properties.setTokenDailyChecks(TOKEN);
        properties.setChannelIdDailyChecks(CHANNEL);
        slackClient = new SlackClient(properties);
    }

    @Test
    void shouldSkipSlackCallWhenTokenMissing() {
        properties.setTokenDailyChecks(" ");

        try (MockedStatic<Slack> slackStatic = mockStatic(Slack.class)) {
            slackClient.sendSlackMessage("hello");
            slackStatic.verifyNoInteractions();
        }
    }

    @Test
    void shouldSendSlackMessageWhenResponseIsOk() throws Exception {
        try (MockedStatic<Slack> slackStatic = mockStatic(Slack.class)) {
            slackStatic.when(Slack::getInstance).thenReturn(slack);
            when(slack.methods(TOKEN)).thenReturn(methodsClient);
            when(methodsClient.chatPostMessage(anyRequestConfigurator())).thenAnswer(invocation -> {
                RequestConfigurator<ChatPostMessageRequest.ChatPostMessageRequestBuilder> configurator =
                    invocation.getArgument(0);
                ChatPostMessageRequest request = configurator.configure(ChatPostMessageRequest.builder()).build();
                assertThat(request.getChannel()).isEqualTo(CHANNEL);
                assertThat(request.getText()).isEqualTo("hello");
                return response;
            });
            when(response.isOk()).thenReturn(true);

            slackClient.sendSlackMessage("hello");

            verify(slack).methods(TOKEN);
            verify(methodsClient).chatPostMessage(anyRequestConfigurator());
            verify(response).isOk();
        }
    }

    @Test
    void shouldThrowWhenSlackApiReturnsErrorResponse() throws Exception {
        try (MockedStatic<Slack> slackStatic = mockStatic(Slack.class)) {
            slackStatic.when(Slack::getInstance).thenReturn(slack);
            when(slack.methods(TOKEN)).thenReturn(methodsClient);
            when(methodsClient.chatPostMessage(anyRequestConfigurator())).thenReturn(response);
            when(response.isOk()).thenReturn(false);
            when(response.getError()).thenReturn("invalid_auth");

            assertThatThrownBy(() -> slackClient.sendSlackMessage("hello"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Slack API error: invalid_auth");
        }
    }

    @Test
    void shouldSwallowIOExceptionFromSlackClient() throws Exception {
        try (MockedStatic<Slack> slackStatic = mockStatic(Slack.class)) {
            slackStatic.when(Slack::getInstance).thenReturn(slack);
            when(slack.methods(TOKEN)).thenReturn(methodsClient);
            when(methodsClient.chatPostMessage(anyRequestConfigurator())).thenThrow(new IOException("network error"));

            assertThatCode(() -> slackClient.sendSlackMessage("hello")).doesNotThrowAnyException();
        }
    }

    @Test
    void shouldSwallowSlackApiException() throws Exception {
        try (MockedStatic<Slack> slackStatic = mockStatic(Slack.class)) {
            slackStatic.when(Slack::getInstance).thenReturn(slack);
            when(slack.methods(TOKEN)).thenReturn(methodsClient);
            when(methodsClient.chatPostMessage(anyRequestConfigurator())).thenThrow(mock(SlackApiException.class));

            assertThatCode(() -> slackClient.sendSlackMessage("hello")).doesNotThrowAnyException();
        }
    }

    private RequestConfigurator<ChatPostMessageRequest.ChatPostMessageRequestBuilder> anyRequestConfigurator() {
        return argThat(configurator -> true);
    }
}




