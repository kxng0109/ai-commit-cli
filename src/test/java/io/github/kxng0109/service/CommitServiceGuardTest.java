package io.github.kxng0109.service;

import io.github.kxng0109.config.UserPreferences;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

/**
 * Guards for fail-closed consent and diff policy in {@link CommitService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CommitService guards")
public class CommitServiceGuardTest {

    @Mock
    private GitService gitService;

    @Mock
    private ChatModel chatModel;

    @BeforeEach
    void setUp() {
        UserPreferences.reset();
    }

    @AfterEach
    void cleanup() {
        UserPreferences.reset();
    }

    @Test
    @DisplayName("secret diff aborts before AI call")
    void secretDiff_abortsBeforeAiCall() {
        CommitService commitService = new CommitService(gitService, chatModel);
        when(gitService.getStagedDiff()).thenReturn("+-----BEGIN PRIVATE KEY-----\n+MIIE");

        assertThatThrownBy(commitService::generateAndCommit)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Refusing to send diff");

        verify(gitService).getStagedDiff();
        verify(chatModel, never()).call(any(Prompt.class));
        verify(gitService, never()).commit(anyString());
    }

    @Test
    @DisplayName("oversize diff aborts before AI call")
    void oversizeDiff_abortsBeforeAiCall() {
        CommitService commitService = new CommitService(gitService, chatModel);
        when(gitService.getStagedDiff()).thenReturn("a".repeat(SecretScanner.MAX_DIFF_BYTES + 1));

        assertThatThrownBy(commitService::generateAndCommit)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Refusing to send diff");

        verify(chatModel, never()).call(any(Prompt.class));
        verify(gitService, never()).commit(anyString());
    }

    @Test
    @DisplayName("lockfile diff aborts before AI call")
    void lockfileDiff_abortsBeforeAiCall() {
        CommitService commitService = new CommitService(gitService, chatModel);
        when(gitService.getStagedDiff())
                .thenReturn("diff --git a/go.sum b/go.sum\n+++ b/go.sum\n+line");

        assertThatThrownBy(commitService::generateAndCommit)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Refusing to send diff");

        verify(chatModel, never()).call(any(Prompt.class));
        verify(gitService, never()).commit(anyString());
    }

    @Test
    @DisplayName("end-of-stream cancels the commit")
    void endOfStream_cancelsCommit() {
        BufferedReader eofReader = new BufferedReader(new StringReader(""));
        CommitService commitService = new CommitService(gitService, chatModel, eofReader);
        when(gitService.getStagedDiff()).thenReturn("diff");
        when(chatModel.call(any(Prompt.class))).thenReturn(response("feat: add feature"));

        commitService.generateAndCommit();

        verify(gitService, never()).commit(anyString());
    }

    @Test
    @DisplayName("read failure cancels the commit")
    void readFailure_cancelsCommit() throws IOException {
        Reader failing = new Reader() {
            @Override
            public int read(char[] buffer, int offset, int length) throws IOException {
                throw new IOException("boom");
            }

            @Override
            public void close() {
            }
        };
        CommitService commitService =
                new CommitService(gitService, chatModel, new BufferedReader(failing));
        when(gitService.getStagedDiff()).thenReturn("diff");
        when(chatModel.call(any(Prompt.class))).thenReturn(response("feat: add feature"));

        commitService.generateAndCommit();

        verify(gitService, never()).commit(anyString());
    }

    @Test
    @DisplayName("full yes word commits")
    void yesWord_commits() {
        BufferedReader reader = new BufferedReader(new StringReader("yes\n"));
        CommitService commitService = new CommitService(gitService, chatModel, reader);
        when(gitService.getStagedDiff()).thenReturn("diff");
        when(chatModel.call(any(Prompt.class))).thenReturn(response("feat: add feature"));
        when(gitService.commit("feat: add feature")).thenReturn("ok");

        commitService.generateAndCommit();

        verify(gitService).commit("feat: add feature");
    }

    @Test
    @DisplayName("edit cancel aborts without commit")
    void editCancel_abortsWithoutCommit() {
        BufferedReader reader = new BufferedReader(new StringReader("e\nc\n"));
        CommitService commitService = new CommitService(gitService, chatModel, reader);
        when(gitService.getStagedDiff()).thenReturn("diff");
        when(chatModel.call(any(Prompt.class))).thenReturn(response("feat: add feature"));

        commitService.generateAndCommit();

        verify(gitService, never()).commit(anyString());
    }

    @Test
    @DisplayName("binary diff aborts before AI call")
    void binaryDiff_abortsBeforeAiCall() {
        CommitService commitService = new CommitService(gitService, chatModel);
        when(gitService.getStagedDiff()).thenReturn("diff  content \u0000 here");

        assertThatThrownBy(commitService::generateAndCommit)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Refusing to send diff");

        verify(chatModel, never()).call(any(Prompt.class));
    }

    @Test
    @DisplayName("abort message carries no secret content")
    void abortMessage_carriesNoSecretContent() {
        String secret = "AKIAIOSFODNN7EXAMPLE";
        CommitService commitService = new CommitService(gitService, chatModel);
        when(gitService.getStagedDiff()).thenReturn("+" + secret);

        assertThatThrownBy(commitService::generateAndCommit)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AWS_ACCESS_KEY");

        try {
            commitService.generateAndCommit();
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).doesNotContain(secret);
        }
    }

    private ChatResponse response(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }
}
