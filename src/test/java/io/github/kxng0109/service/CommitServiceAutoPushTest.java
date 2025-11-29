package io.github.kxng0109.service;

import io.github.kxng0109.config.UserPreferences;
import org.junit.jupiter.api.AfterEach;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CommitServiceAutoPushTest {

    @Mock
    private GitService gitService;

    @Mock
    private ChatModel chatModel;

    private CommitService commitService;

    @BeforeEach
    void setUp() {
        UserPreferences.reset();
    }

    @AfterEach
    void cleanup() {
        UserPreferences.reset();
    }


    @Test
    void generateAndCommit_withBothDisabled_shouldNotPush() {
        UserPreferences.setAutoCommit(false);
        UserPreferences.setAutoPush(false);

        BufferedReader mockReader = new BufferedReader(new StringReader("y\n"));
        commitService = new CommitService(gitService, chatModel, mockReader);

        when(gitService.hasStagedChanges()).thenReturn(true);
        when(gitService.getStagedDiff()).thenReturn("diff");
        when(gitService.commit(anyString())).thenReturn("success");

        ChatResponse mockResponse = createMockChatResponse("feat: test");
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        commitService.generateAndCommit();

        verify(gitService).commit("feat: test");
        verify(gitService, never()).push();
    }

    @Test
    void generateAndCommit_withInteractiveModeAndAutoPush_shouldPushAfterAccept() {
        UserPreferences.setAutoCommit(false);
        UserPreferences.setAutoPush(true);

        BufferedReader mockReader = new BufferedReader(new StringReader("y\n"));
        commitService = new CommitService(gitService, chatModel, mockReader);

        when(gitService.hasStagedChanges()).thenReturn(true);
        when(gitService.getStagedDiff()).thenReturn("diff");
        when(gitService.commit(anyString())).thenReturn("success");
        when(gitService.push()).thenReturn("pushed");

        ChatResponse mockResponse = createMockChatResponse("feat: test");
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        commitService.generateAndCommit();

        verify(gitService).commit("feat: test");
        verify(gitService).push();
    }

    @Test
    void generateAndCommit_withInteractiveModeAutoPushAndRegenerate_shouldOnlyPushOnAccept() {
        UserPreferences.setAutoCommit(false);
        UserPreferences.setAutoPush(true);

        BufferedReader mockReader = new BufferedReader(new StringReader("r\ny\n"));
        commitService = new CommitService(gitService, chatModel, mockReader);

        when(gitService.hasStagedChanges()).thenReturn(true);
        when(gitService.getStagedDiff()).thenReturn("diff");
        when(gitService.commit(anyString())).thenReturn("success");
        when(gitService.push()).thenReturn("pushed");

        ChatResponse firstResponse = createMockChatResponse("feat: first");
        ChatResponse secondResponse = createMockChatResponse("feat: second");
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(firstResponse)
                .thenReturn(secondResponse);

        commitService.generateAndCommit();

        verify(gitService).commit("feat: second");
        verify(gitService, times(1)).push();  // Only push once after final accept
    }

    @Test
    void generateAndCommit_withInteractiveModeAutoPushAndEdit_shouldPushEditedMessage() {
        UserPreferences.setAutoCommit(false);
        UserPreferences.setAutoPush(true);

        String editedMessage = "feat: custom edited message";
        BufferedReader mockReader = new BufferedReader(new StringReader("e\n" + editedMessage + "\n"));
        commitService = new CommitService(gitService, chatModel, mockReader);

        when(gitService.hasStagedChanges()).thenReturn(true);
        when(gitService.getStagedDiff()).thenReturn("diff");
        when(gitService.commit(editedMessage)).thenReturn("success");
        when(gitService.push()).thenReturn("pushed");

        ChatResponse mockResponse = createMockChatResponse("feat: original");
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        commitService.generateAndCommit();

        verify(gitService).commit(editedMessage);
        verify(gitService).push();
    }

    @Test
    void generateAndCommit_withInteractiveModeAutoPushAndCancel_shouldNotCommitOrPush() {
        UserPreferences.setAutoCommit(false);
        UserPreferences.setAutoPush(true);

        BufferedReader mockReader = new BufferedReader(new StringReader("c\n"));
        commitService = new CommitService(gitService, chatModel, mockReader);

        when(gitService.hasStagedChanges()).thenReturn(true);
        when(gitService.getStagedDiff()).thenReturn("diff");

        ChatResponse mockResponse = createMockChatResponse("feat: test");
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        commitService.generateAndCommit();

        verify(gitService, never()).commit(anyString());
        verify(gitService, never()).push();
    }

    @Test
    void generateAndCommit_withInteractiveModeAutoPushButPushFails_shouldStillCommit() {
        UserPreferences.setAutoCommit(false);
        UserPreferences.setAutoPush(true);

        BufferedReader mockReader = new BufferedReader(new StringReader("y\n"));
        commitService = new CommitService(gitService, chatModel, mockReader);

        when(gitService.hasStagedChanges()).thenReturn(true);
        when(gitService.getStagedDiff()).thenReturn("diff");
        when(gitService.commit(anyString())).thenReturn("success");
        when(gitService.push()).thenThrow(new RuntimeException("Push failed"));

        ChatResponse mockResponse = createMockChatResponse("feat: test");
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        assertDoesNotThrow(() -> commitService.generateAndCommit());

        verify(gitService).commit("feat: test");
        verify(gitService).push();
    }

    @Test
    void generateAndCommit_withAutoCommitOnlyAndAutoPushOff_shouldCommitButNotPush() {
        UserPreferences.setAutoCommit(true);
        UserPreferences.setAutoPush(false);

        commitService = new CommitService(gitService, chatModel);

        when(gitService.hasStagedChanges()).thenReturn(true);
        when(gitService.getStagedDiff()).thenReturn("diff");
        when(gitService.commit(anyString())).thenReturn("success");

        ChatResponse mockResponse = createMockChatResponse("feat: test");
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        commitService.generateAndCommit();

        verify(gitService).commit("feat: test");
        verify(gitService, never()).push();
    }

    @Test
    void generateAndCommit_withBothEnabled_shouldCommitAndPush() {
        UserPreferences.setAutoCommit(true);
        UserPreferences.setAutoPush(true);

        commitService = new CommitService(gitService, chatModel);

        when(gitService.hasStagedChanges()).thenReturn(true);
        when(gitService.getStagedDiff()).thenReturn("diff");
        when(gitService.commit(anyString())).thenReturn("success");
        when(gitService.push()).thenReturn("pushed");

        ChatResponse mockResponse = createMockChatResponse("feat: test");
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        commitService.generateAndCommit();

        verify(gitService).commit("feat: test");
        verify(gitService).push();
    }

    @Test
    void generateAndCommit_withBothEnabledButPushFails_shouldStillCommit() {
        UserPreferences.setAutoCommit(true);
        UserPreferences.setAutoPush(true);

        commitService = new CommitService(gitService, chatModel);

        when(gitService.hasStagedChanges()).thenReturn(true);
        when(gitService.getStagedDiff()).thenReturn("diff");
        when(gitService.commit(anyString())).thenReturn("success");
        when(gitService.push()).thenThrow(new RuntimeException("Push failed"));

        ChatResponse mockResponse = createMockChatResponse("feat: test");
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        assertDoesNotThrow(() -> commitService.generateAndCommit());

        verify(gitService).commit("feat: test");
        verify(gitService).push();
    }

    @Test
    void generateAndCommit_withBothEnabledButAiFails_shouldNotCommitOrPush() {
        UserPreferences.setAutoCommit(true);
        UserPreferences.setAutoPush(true);

        commitService = new CommitService(gitService, chatModel);

        when(gitService.hasStagedChanges()).thenReturn(true);
        when(gitService.getStagedDiff()).thenReturn("diff");
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("AI error"));

        assertThrows(RuntimeException.class, () -> commitService.generateAndCommit());

        verify(gitService, never()).commit(anyString());
        verify(gitService, never()).push();
    }

    @Test
    void generateAndCommit_withAutoPushOnEmptyEditInInteractiveMode_shouldUseOriginalAndPush() {
        UserPreferences.setAutoCommit(false);
        UserPreferences.setAutoPush(true);

        BufferedReader mockReader = new BufferedReader(new StringReader("e\n\n"));
        commitService = new CommitService(gitService, chatModel, mockReader);

        when(gitService.hasStagedChanges()).thenReturn(true);
        when(gitService.getStagedDiff()).thenReturn("diff");
        when(gitService.commit("feat: original")).thenReturn("success");
        when(gitService.push()).thenReturn("pushed");

        ChatResponse mockResponse = createMockChatResponse("feat: original");
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        commitService.generateAndCommit();

        verify(gitService).commit("feat: original");
        verify(gitService).push();
    }

    @Test
    void generateAndCommit_withMultipleRegenerationsAndAutoPush_shouldOnlyPushOnce() {
        UserPreferences.setAutoCommit(false);
        UserPreferences.setAutoPush(true);

        BufferedReader mockReader = new BufferedReader(new StringReader("r\nr\nr\ny\n"));
        commitService = new CommitService(gitService, chatModel, mockReader);

        when(gitService.hasStagedChanges()).thenReturn(true);
        when(gitService.getStagedDiff()).thenReturn("diff");
        when(gitService.commit(anyString())).thenReturn("success");
        when(gitService.push()).thenReturn("pushed");

        ChatResponse resp1 = createMockChatResponse("feat: v1");
        ChatResponse resp2 = createMockChatResponse("feat: v2");
        ChatResponse resp3 = createMockChatResponse("feat: v3");
        ChatResponse resp4 = createMockChatResponse("feat: v4");
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(resp1)
                .thenReturn(resp2)
                .thenReturn(resp3)
                .thenReturn(resp4);

        commitService.generateAndCommit();

        verify(gitService).commit("feat: v4");
        verify(gitService, times(1)).push();  // Only once after final accept
    }


    private ChatResponse createMockChatResponse(String text) {
        AssistantMessage message = new AssistantMessage(text);
        Generation generation = new Generation(message);
        return new ChatResponse(List.of(generation));
    }
}