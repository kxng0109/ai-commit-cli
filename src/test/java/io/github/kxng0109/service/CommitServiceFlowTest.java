package io.github.kxng0109.service;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

/**
 * Guards for preview, forced auto, and amend flows in {@link CommitService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CommitService flows")
public class CommitServiceFlowTest {

    @Mock
    private GitService gitService;

    @Mock
    private ChatModel chatModel;

    @Test
    @DisplayName("preview returns the message without committing")
    void preview_returnsWithoutCommitting() {
        CommitService service = new CommitService(gitService, chatModel);
        when(gitService.getStagedDiff()).thenReturn("diff");
        when(chatModel.call(any(Prompt.class))).thenReturn(response("feat: add feature"));

        String message = service.previewMessage();

        assertThat(message).isEqualTo("feat: add feature");
        verify(gitService, never()).commit(anyString());
        verify(gitService, never()).amend(anyString());
    }

    @Test
    @DisplayName("forced auto commits without prompting")
    void forcedAuto_commitsWithoutPrompting() {
        BufferedReader eofReader = new BufferedReader(new StringReader(""));
        CommitService service = new CommitService(gitService, chatModel, eofReader);
        when(gitService.getStagedDiff()).thenReturn("diff");
        when(chatModel.call(any(Prompt.class))).thenReturn(response("feat: add feature"));
        when(gitService.commit("feat: add feature")).thenReturn("ok");

        service.generateAndCommit(true);

        verify(gitService).commit("feat: add feature");
    }

    @Test
    @DisplayName("forced auto amend calls amend")
    void forcedAutoAmend_callsAmend() {
        CommitService service = new CommitService(gitService, chatModel);
        when(gitService.getStagedDiff()).thenReturn("diff");
        when(chatModel.call(any(Prompt.class))).thenReturn(response("feat: add feature"));
        when(gitService.amend("feat: add feature")).thenReturn("ok");

        service.generateAndAmend(true);

        verify(gitService).amend("feat: add feature");
        verify(gitService, never()).commit(anyString());
    }

    @Test
    @DisplayName("interactive amend commits via amend on accept")
    void interactiveAmend_amendsOnAccept() {
        BufferedReader reader = new BufferedReader(new StringReader("y\n"));
        CommitService service = new CommitService(gitService, chatModel, reader);
        when(gitService.getStagedDiff()).thenReturn("diff");
        when(chatModel.call(any(Prompt.class))).thenReturn(response("feat: add feature"));
        when(gitService.amend("feat: add feature")).thenReturn("ok");

        service.generateAndAmend(false);

        verify(gitService).amend("feat: add feature");
        verify(gitService, never()).commit(anyString());
    }

    @Test
    @DisplayName("explicit commit flag still commits normally")
    void explicitCommit_commitsNormally() {
        BufferedReader reader = new BufferedReader(new StringReader("yes\n"));
        CommitService service = new CommitService(gitService, chatModel, reader);
        when(gitService.getStagedDiff()).thenReturn("diff");
        when(chatModel.call(any(Prompt.class))).thenReturn(response("feat: add feature"));
        when(gitService.commit("feat: add feature")).thenReturn("ok");

        service.generateAndCommit(false);

        verify(gitService).commit("feat: add feature");
        verify(gitService, never()).amend(anyString());
    }

    private ChatResponse response(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }
}
