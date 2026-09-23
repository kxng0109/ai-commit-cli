package io.github.kxng0109;

import io.github.kxng0109.config.Config;
import io.github.kxng0109.config.UserPreferences;
import io.github.kxng0109.service.GitService;

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
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * Guards for flow dispatch in {@link AiCommitCli}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AiCommitCli dispatch")
public class AiCommitCliDispatchTest {

    @Mock
    private GitService gitService;

    @Mock
    private ChatModel chatModel;

    private static Config openaiConfig() {
        return new Config(
                new Config.OpenAiConfig("key", "https://api.openai.com", "gpt-4o-mini"),
                new Config.AnthropicConfig(null, "claude-sonnet-4-0"),
                new Config.GoogleConfig(null, "gemini-2.0-flash"),
                new Config.DeepseekConfig(null),
                new Config.OllamaConfig(null, "http://localhost:11434"),
                0.1,
                30);
    }

    private static AiCommitCli.CliOptions options(boolean yes, boolean dryRun, boolean amend,
            boolean stageAll, String model, String provider) {
        return new AiCommitCli.CliOptions(false, false, yes, dryRun, amend, stageAll, model,
                provider, null, List.of());
    }

    @Test
    @DisplayName("resolve without overrides uses priority provider")
    void resolveWithoutOverrides_usesPriority() {
        ChatModel resolved = AiCommitCli.resolveChatModel(openaiConfig(), options(false, false, false, false, null, null));

        assertThat(resolved).isNotNull();
    }

    @Test
    @DisplayName("resolve with forced provider selects it")
    void resolveForced_selectsIt() {
        Config config = new Config(
                openaiConfig().openai(), new Config.AnthropicConfig("key", "claude-sonnet-4-0"),
                openaiConfig().google(), openaiConfig().deepseek(), openaiConfig().ollama(), 0.1, 30);

        ChatModel resolved = AiCommitCli.resolveChatModel(
                config, options(false, false, false, false, null, "anthropic"));

        assertThat(resolved).isInstanceOf(AnthropicChatModel.class);
    }

    @Test
    @DisplayName("resolve with model only targets priority provider")
    void resolveModelOnly_targetsPriority() {
        ChatModel resolved = AiCommitCli.resolveChatModel(openaiConfig(), options(false, false, false, false, "gpt-4o", null));

        assertThat(resolved.getDefaultOptions().getModel()).isEqualTo("gpt-4o");
    }

    @Test
    @DisplayName("resolve with provider and model applies both")
    void resolveBoth_appliesBoth() {
        Config config = new Config(
                openaiConfig().openai(), new Config.AnthropicConfig("key", "old"),
                openaiConfig().google(), openaiConfig().deepseek(), openaiConfig().ollama(), 0.1, 30);

        ChatModel resolved = AiCommitCli.resolveChatModel(config, options(false, false, false, false, "new", "anthropic"));

        assertThat(resolved.getDefaultOptions().getModel()).isEqualTo("new");
    }

    @Test
    @DisplayName("stage-all stages before dry run")
    void stageAll_beforeDryRun() {
        when(gitService.getStagedDiff()).thenReturn("diff");
        when(chatModel.call(any(Prompt.class))).thenReturn(response("feat: x"));

        AiCommitCli.executeCommit(options(false, true, false, true, null, null), gitService, chatModel);

        verify(gitService).stageTracked();
        verify(gitService, never()).commit(anyString());
    }

    @Test
    @DisplayName("dry run previews without committing")
    void dryRun_previews() {
        when(gitService.getStagedDiff()).thenReturn("diff");
        when(chatModel.call(any(Prompt.class))).thenReturn(response("feat: x"));

        AiCommitCli.executeCommit(options(false, true, false, false, null, null), gitService, chatModel);

        verify(gitService, never()).commit(anyString());
        verify(gitService, never()).amend(anyString());
    }

    @Test
    @DisplayName("amend with yes calls amend")
    void amendYes_callsAmend() {
        when(gitService.getStagedDiff()).thenReturn("diff");
        when(chatModel.call(any(Prompt.class))).thenReturn(response("feat: x"));

        AiCommitCli.executeCommit(options(true, false, true, false, null, null), gitService, chatModel);

        verify(gitService).amend("feat: x");
    }

    @Test
    @DisplayName("yes commits without prompting")
    void yes_commits() {
        when(gitService.getStagedDiff()).thenReturn("diff");
        when(chatModel.call(any(Prompt.class))).thenReturn(response("feat: x"));

        AiCommitCli.executeCommit(options(true, false, false, false, null, null), gitService, chatModel);

        verify(gitService).commit("feat: x");
    }

    @Test
    @DisplayName("plain flags use preference flow")
    void plain_usesPreferences() {
        UserPreferences.reset();
        UserPreferences.setAutoCommit(true);
        try {
            when(gitService.getStagedDiff()).thenReturn("diff");
            when(chatModel.call(any(Prompt.class))).thenReturn(response("feat: x"));

            AiCommitCli.executeCommit(options(false, false, false, false, null, null), gitService, chatModel);

            verify(gitService).commit("feat: x");
        } finally {
            UserPreferences.reset();
        }
    }

    @Test
    @DisplayName("run covers remaining shells")
    void run_coversShells() {
        assertThat(AiCommitCli.run(new String[]{"completion", "zsh"})).isZero();
        assertThat(AiCommitCli.run(new String[]{"completion", "fish"})).isZero();
        assertThat(AiCommitCli.run(new String[]{"completion", "powershell"})).isZero();
    }

    @Test
    @DisplayName("run with flags but no provider fails safely")
    void runFlags_noProviderFails() {
        assumeNoProvider();
        assertThat(AiCommitCli.run(new String[]{"--dry-run"})).isOne();
        assertThat(AiCommitCli.run(new String[]{"--yes"})).isOne();
        assertThat(AiCommitCli.run(new String[]{"--amend"})).isOne();
    }

    private static void assumeNoProvider() {
        org.junit.jupiter.api.Assumptions.assumeTrue(System.getenv("OPENAI_API_KEY") == null
                && System.getenv("ANTHROPIC_API_KEY") == null
                && System.getenv("GOOGLE_API_KEY") == null
                && System.getenv("DEEPSEEK_API_KEY") == null
                && System.getenv("OLLAMA_MODEL") == null);
    }

    private ChatResponse response(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }
}
