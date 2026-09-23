package io.github.kxng0109.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
 * Guards for commit edge paths in {@link CommitService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CommitService edges")
public class CommitServiceEdgeTest {

    @Mock
    private GitService gitService;

    @Mock
    private ChatModel chatModel;

    @Test
    @DisplayName("null diff aborts as no staged changes")
    void nullDiff_abortsAsNoChanges() {
        CommitService service = new CommitService(gitService, chatModel);
        when(gitService.getStagedDiff()).thenReturn(null);

        assertThatThrownBy(service::generateAndCommit)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No staged changes");
    }

    @Test
    @DisplayName("null generation output aborts as empty")
    void nullGenerationOutput_abortsAsEmpty() {
        CommitService service = new CommitService(gitService, chatModel);
        when(gitService.getStagedDiff()).thenReturn("diff");
        Generation generation = mock(Generation.class);
        when(generation.getOutput()).thenReturn(null);
        when(chatModel.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(generation)));

        assertThatThrownBy(service::generateAndCommit)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no message output");
    }

    @Test
    @DisplayName("server error falls back to generic failure")
    void serverError_fallsBackToGeneric() {
        CommitService service = new CommitService(gitService, chatModel);
        when(gitService.getStagedDiff()).thenReturn("diff");
        HttpClientErrorException error =
                new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("call failed", error));

        assertThatThrownBy(service::generateAndCommit)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to generate a commit message");
    }

    @Test
    @DisplayName("direct interrupt aborts and restores flag")
    void directInterrupt_abortsAndRestoresFlag() {
        ChatModel interrupting = prompt -> {
            CommitServiceEdgeTest.<RuntimeException>sneakyThrow(new InterruptedException("interrupted"));
            return null;
        };
        CommitService service = new CommitService(gitService, interrupting);
        when(gitService.getStagedDiff()).thenReturn("diff");

        try {
            assertThatThrownBy(service::generateAndCommit)
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("interrupted");
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    @DisplayName("cancel word aborts the edit")
    void cancelWord_abortsEdit() {
        BufferedReader reader = new BufferedReader(new StringReader("e\ncancel\n"));
        CommitService service = new CommitService(gitService, chatModel, reader);
        when(gitService.getStagedDiff()).thenReturn("diff");
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("feat: add feature")))));

        service.generateAndCommit();

        verify(gitService, never()).commit(anyString());
    }

    @Test
    @DisplayName("edit read failure cancels the edit")
    void editReadFailure_cancelsEdit() {
        Reader flaky = new Reader() {
            private int calls;

            @Override
            public int read(char[] buffer, int offset, int length) throws IOException {
                if (calls++ == 0) {
                    String line = "e\n";
                    line.getChars(0, line.length(), buffer, offset);
                    return line.length();
                }
                throw new IOException("boom");
            }

            @Override
            public void close() {
            }
        };
        CommitService service =
                new CommitService(gitService, chatModel, new BufferedReader(flaky));
        when(gitService.getStagedDiff()).thenReturn("diff");
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("feat: add feature")))));

        service.generateAndCommit();

        verify(gitService, never()).commit(anyString());
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable throwable) throws T {
        throw (T) throwable;
    }
}
