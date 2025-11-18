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

import java.io.BufferedReader;
import java.io.StringReader;
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

    @Test
    void constructor_shouldCreateService() {
        CommitService service = new CommitService(gitService, chatModel);

        assertNotNull(service);
    }

    @Test
    void generateAndCommit_withNoStagedChanges_shouldThrowException() {
        commitService = new CommitService(gitService, chatModel);
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

        BufferedReader mockReader = new BufferedReader(new StringReader("y\n"));
        commitService = new CommitService(gitService, chatModel, mockReader);

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

        commitService = new CommitService(gitService, chatModel);
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

        commitService = new CommitService(gitService, chatModel);
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

        commitService = new CommitService(gitService, chatModel);
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

        commitService = new CommitService(gitService, chatModel);
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

        BufferedReader mockReader = new BufferedReader(new StringReader("y\n"));
        commitService = new CommitService(gitService, chatModel, mockReader);

        when(gitService.hasStagedChanges()).thenReturn(true);
        when(gitService.getStagedDiff()).thenReturn(diff);
        when(gitService.commit(trimmedMessage)).thenReturn(commitOutput);

        ChatResponse mockResponse = createMockChatResponse(aiMessageWithWhitespace);
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        commitService.generateAndCommit();

        verify(gitService).commit(trimmedMessage);
    }

    @Test
    void generateAndCommit_withUserPressingR_shouldRegenerateMessage() {
        String diff = "diff --git a/test.txt\n+change";
        String firstMessage = "feat: add initial feature";
        String secondMessage = "feat: add improved feature";
        String commitOutput = "[main abc123] feat: add improved feature";

        BufferedReader mockReader = new BufferedReader(new StringReader("r\ny\n"));
        commitService = new CommitService(gitService, chatModel, mockReader);

        when(gitService.hasStagedChanges()).thenReturn(true);
        when(gitService.getStagedDiff()).thenReturn(diff);
        when(gitService.commit(secondMessage)).thenReturn(commitOutput);

        ChatResponse firstResponse = createMockChatResponse(firstMessage);
        ChatResponse secondResponse = createMockChatResponse(secondMessage);
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(firstResponse)
                .thenReturn(secondResponse);

        commitService.generateAndCommit();

        verify(gitService).hasStagedChanges();
        verify(gitService).getStagedDiff();
        verify(chatModel, times(2)).call(any(Prompt.class));
        verify(gitService).commit(secondMessage);
    }

    @Test
    void generateAndCommit_withUserPressingE_shouldCommitEditedMessage() {
        String diff = "diff --git a/test.txt\n+change";
        String aiMessage = "feat: add feature";
        String editedMessage = "feat: add custom feature with more details";
        String commitOutput = "[main abc123] feat: add custom feature with more details";

        BufferedReader mockReader = new BufferedReader(new StringReader("e\n" + editedMessage + "\n"));
        commitService = new CommitService(gitService, chatModel, mockReader);

        when(gitService.hasStagedChanges()).thenReturn(true);
        when(gitService.getStagedDiff()).thenReturn(diff);
        when(gitService.commit(editedMessage)).thenReturn(commitOutput);

        ChatResponse mockResponse = createMockChatResponse(aiMessage);
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        commitService.generateAndCommit();

        verify(gitService).hasStagedChanges();
        verify(gitService).getStagedDiff();
        verify(chatModel).call(any(Prompt.class));
        verify(gitService).commit(editedMessage);
    }

    @Test
    void generateAndCommit_withUserPressingE_andEmptyEdit_shouldUseOriginalMessage() {
        String diff = "diff --git a/test.txt\n+change";
        String aiMessage = "feat: add feature";
        String commitOutput = "[main abc123] feat: add feature";

        BufferedReader mockReader = new BufferedReader(new StringReader("e\n\n"));
        commitService = new CommitService(gitService, chatModel, mockReader);

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
    void generateAndCommit_withUserPressingC_shouldCancelCommit() {
        String diff = "diff --git a/test.txt\n+change";
        String aiMessage = "feat: add feature";

        BufferedReader mockReader = new BufferedReader(new StringReader("c\n"));
        commitService = new CommitService(gitService, chatModel, mockReader);

        when(gitService.hasStagedChanges()).thenReturn(true);
        when(gitService.getStagedDiff()).thenReturn(diff);

        ChatResponse mockResponse = createMockChatResponse(aiMessage);
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        commitService.generateAndCommit();

        verify(gitService).hasStagedChanges();
        verify(gitService).getStagedDiff();
        verify(chatModel).call(any(Prompt.class));
        verify(gitService, never()).commit(anyString());
    }

    @Test
    void generateAndCommit_withEmptyUserInput_shouldDefaultToYes() {
        String diff = "diff --git a/test.txt\n+change";
        String aiMessage = "feat: add feature";
        String commitOutput = "[main abc123] feat: add feature";

        BufferedReader mockReader = new BufferedReader(new StringReader("\n"));
        commitService = new CommitService(gitService, chatModel, mockReader);

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
    void generateAndCommit_withInvalidChoice_thenValidChoice_shouldRetryAndCommit() {
        String diff = "diff --git a/test.txt\n+change";
        String aiMessage = "feat: add feature";
        String commitOutput = "[main abc123] feat: add feature";

        BufferedReader mockReader = new BufferedReader(new StringReader("x\ny\n"));
        commitService = new CommitService(gitService, chatModel, mockReader);

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
    void generateAndCommit_withMultipleRegenerations_shouldCommitFinalMessage() {
        String diff = "diff --git a/test.txt\n+change";
        String firstMessage = "feat: add feature v1";
        String secondMessage = "feat: add feature v2";
        String thirdMessage = "feat: add feature v3";
        String commitOutput = "[main abc123] feat: add feature v3";

        BufferedReader mockReader = new BufferedReader(new StringReader("r\nr\ny\n"));
        commitService = new CommitService(gitService, chatModel, mockReader);

        when(gitService.hasStagedChanges()).thenReturn(true);
        when(gitService.getStagedDiff()).thenReturn(diff);
        when(gitService.commit(thirdMessage)).thenReturn(commitOutput);

        ChatResponse firstResponse = createMockChatResponse(firstMessage);
        ChatResponse secondResponse = createMockChatResponse(secondMessage);
        ChatResponse thirdResponse = createMockChatResponse(thirdMessage);
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(firstResponse)
                .thenReturn(secondResponse)
                .thenReturn(thirdResponse);

        commitService.generateAndCommit();

        verify(gitService).hasStagedChanges();
        verify(gitService).getStagedDiff();
        verify(chatModel, times(3)).call(any(Prompt.class));
        verify(gitService).commit(thirdMessage);
    }

    private ChatResponse createMockChatResponse(String text) {
        AssistantMessage message = new AssistantMessage(text);
        Generation generation = new Generation(message);
        return new ChatResponse(List.of(generation));
    }
}
