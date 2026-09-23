package io.github.kxng0109.service;

import java.io.BufferedReader;
import java.io.StringReader;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Guards for AI failure mapping in {@link CommitService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CommitService reliability")
public class CommitServiceReliabilityTest {

    @Mock
    private GitService gitService;

    @Mock
    private ChatModel chatModel;

    @Test
    @DisplayName("unresolvable host maps to actionable error")
    void unknownHost_mapsToActionableError() {
        CommitService service = new CommitService(gitService, chatModel);
        when(gitService.getStagedDiff()).thenReturn("diff");
        when(chatModel.call(any(Prompt.class)))
                .thenThrow(new RuntimeException("call failed", new UnknownHostException("ai.example.com")));

        assertThatThrownBy(service::generateAndCommit)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot resolve AI provider host");
    }

    @Test
    @DisplayName("socket timeout maps to actionable error")
    void socketTimeout_mapsToActionableError() {
        CommitService service = new CommitService(gitService, chatModel);
        when(gitService.getStagedDiff()).thenReturn("diff");
        when(chatModel.call(any(Prompt.class)))
                .thenThrow(new RuntimeException("call failed", new SocketTimeoutException("read timed out")));

        assertThatThrownBy(service::generateAndCommit)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("timed out");
    }

    @Test
    @DisplayName("connection refused maps to actionable error")
    void connectFailure_mapsToActionableError() {
        CommitService service = new CommitService(gitService, chatModel);
        when(gitService.getStagedDiff()).thenReturn("diff");
        when(chatModel.call(any(Prompt.class)))
                .thenThrow(new RuntimeException("call failed", new ConnectException("refused")));

        assertThatThrownBy(service::generateAndCommit)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot connect to AI provider");
    }

    @Test
    @DisplayName("401 maps to api key error")
    void unauthorized_mapsToApiKeyError() {
        CommitService service = new CommitService(gitService, chatModel);
        when(gitService.getStagedDiff()).thenReturn("diff");
        HttpClientErrorException error = new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("call failed", error));

        assertThatThrownBy(service::generateAndCommit)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("401");
    }

    @Test
    @DisplayName("429 maps to rate limit error")
    void rateLimited_mapsToRateLimitError() {
        CommitService service = new CommitService(gitService, chatModel);
        when(gitService.getStagedDiff()).thenReturn("diff");
        HttpClientErrorException error = new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS);
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("call failed", error));

        assertThatThrownBy(service::generateAndCommit)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("429");
    }

    @Test
    @DisplayName("null response stage maps to empty message error")
    void nullResponse_mapsToEmptyError() {
        CommitService service = new CommitService(gitService, chatModel);
        when(gitService.getStagedDiff()).thenReturn("diff");
        when(chatModel.call(any(Prompt.class))).thenReturn(null);

        assertThatThrownBy(service::generateAndCommit)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no response");
    }

    @Test
    @DisplayName("null output stage maps to empty message error")
    void nullOutput_mapsToEmptyError() {
        CommitService service = new CommitService(gitService, chatModel);
        when(gitService.getStagedDiff()).thenReturn("diff");
        ChatResponse response = mock(ChatResponse.class);
        when(response.getResult()).thenReturn(null);
        when(chatModel.call(any(Prompt.class))).thenReturn(response);

        assertThatThrownBy(service::generateAndCommit)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no message output");
    }

    @Test
    @DisplayName("null text stage maps to empty message error")
    void nullText_mapsToEmptyError() {
        CommitService service = new CommitService(gitService, chatModel);
        when(gitService.getStagedDiff()).thenReturn("diff");
        Generation generation = mock(Generation.class);
        when(generation.getOutput()).thenReturn(new AssistantMessage((String) null));
        ChatResponse response = new ChatResponse(List.of(generation));
        when(chatModel.call(any(Prompt.class))).thenReturn(response);

        assertThatThrownBy(service::generateAndCommit)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("empty");
    }

    @Test
    @DisplayName("interrupted cause restores flag and aborts")
    void interruptedCause_restoresFlag() {
        BufferedReader reader = new BufferedReader(new StringReader("y\n"));
        CommitService service = new CommitService(gitService, chatModel, reader);
        when(gitService.getStagedDiff()).thenReturn("diff");
        when(chatModel.call(any(Prompt.class)))
                .thenThrow(new RuntimeException("call failed", new InterruptedException("interrupted")));

        try {
            assertThatThrownBy(service::generateAndCommit)
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("interrupted");
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }
}
