package io.github.kxng0109.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CommitServiceTest {
    @Mock
    private GitService gitService;

    @Mock
    private ChatModel chatModel;

    private CommitService commitService;

    @BeforeEach
    void setUp() {
        commitService = new CommitService(gitService, chatModel);
    }

    @Test
    void constructor_shouldCreateService() {
        CommitService service = new CommitService(gitService, chatModel);

        assertNotNull(service);
    }

    @Test
    void generateAndCommit_withNoStagedChanges_shouldThrowException() {
        when(gitService.hasStagedChanges()).thenReturn(false);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> commitService.generateAndCommit()
        );

        assertTrue(exception.getMessage().contains("No staged changes found"));
        verify(gitService).hasStagedChanges();
        verify(gitService, never()).getStagedDiff();
        verify(chatModel, never()).call(any(Prompt.class));
        verify(gitService, never()).commit(anyString());
    }

    @Test
    void generateAndCommit_withStagedChanges_shouldCommitSuccessfully() {
        String diff = "diff --git a/test.txt\n+new line";
        String aiMessage = "feat: add new feature";
        String commitOutput = "[main abc123] feat: add new feature";

        when(gitService.hasStagedChanges()).thenReturn(true);
        when(gitService.getStagedDiff()).thenReturn(diff);
        when(gitService.commit(aiMessage)).thenReturn(commitOutput);

        ChatResponse mockResponse = createMockChatResponse(aiMessage);
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        commitService.generateAndCommit();

        verify(gitService).hasStagedChanges();
        verify(gitService).getStagedDiff();
        verify(chatModel).call(any(Prompt.class));
        verify(gitService).commit(aiMessage);
    }

    @Test
    void generateAndCommit_withEmptyAiResponse_shouldThrowException() {
        String diff = "diff --git a/test.txt\n+change";

        when(gitService.hasStagedChanges()).thenReturn(true);
        when(gitService.getStagedDiff()).thenReturn(diff);

        ChatResponse mockResponse = createMockChatResponse("");
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> commitService.generateAndCommit()
        );

        assertTrue(exception.getMessage().contains("AI returned an empty commit message"));
        verify(gitService).hasStagedChanges();
        verify(gitService).getStagedDiff();
        verify(chatModel).call(any(Prompt.class));
        verify(gitService, never()).commit(anyString());
    }

    @Test
    void generateAndCommit_withNullAiResponse_shouldThrowException() {
        String diff = "diff --git a/test.txt\n+change";

        when(gitService.hasStagedChanges()).thenReturn(true);
        when(gitService.getStagedDiff()).thenReturn(diff);

        ChatResponse mockResponse = createMockChatResponse(null);
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> commitService.generateAndCommit()
        );

        assertTrue(exception.getMessage().contains("AI returned an empty commit message"));
        verify(gitService, never()).commit(anyString());
    }

    @Test
    void generateAndCommit_withWhitespaceOnlyResponse_shouldThrowException() {
        String diff = "diff --git a/test.txt\n+change";

        when(gitService.hasStagedChanges()).thenReturn(true);
        when(gitService.getStagedDiff()).thenReturn(diff);

        ChatResponse mockResponse = createMockChatResponse("   \n\t  ");
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> commitService.generateAndCommit()
        );

        assertTrue(exception.getMessage().contains("AI returned an empty commit message"));
        verify(gitService, never()).commit(anyString());
    }

    @Test
    void generateAndCommit_withAiException_shouldWrapAndThrow() {
        String diff = "diff --git a/test.txt\n+change";

        when(gitService.hasStagedChanges()).thenReturn(true);
        when(gitService.getStagedDiff()).thenReturn(diff);
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("API error"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> commitService.generateAndCommit()
        );

        assertTrue(exception.getMessage().contains("Failed to generate a commit message"));
        verify(gitService, never()).commit(anyString());
    }

    @Test
    void generateAndCommit_withValidMessageContainingExtraWhitespace_shouldTrimAndCommit() {
        String diff = "diff --git a/test.txt\n+change";
        String aiMessageWithWhitespace = "\n\n  feat: add feature  \n\n";
        String trimmedMessage = "feat: add feature";
        String commitOutput = "[main abc123] feat: add feature";

        when(gitService.hasStagedChanges()).thenReturn(true);
        when(gitService.getStagedDiff()).thenReturn(diff);
        when(gitService.commit(trimmedMessage)).thenReturn(commitOutput);

        ChatResponse mockResponse = createMockChatResponse(aiMessageWithWhitespace);
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        commitService.generateAndCommit();

        verify(gitService).commit(trimmedMessage);
    }

    private ChatResponse createMockChatResponse(String text) {
        AssistantMessage message = new AssistantMessage(text);
        Generation generation = new Generation(message);
        return new ChatResponse(List.of(generation));
    }
}
