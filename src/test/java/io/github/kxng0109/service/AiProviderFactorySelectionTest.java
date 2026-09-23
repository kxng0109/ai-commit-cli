package io.github.kxng0109.service;

import io.github.kxng0109.config.Config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;

/**
 * Guards for provider selection in {@link AiProviderFactory}.
 */
@DisplayName("AiProviderFactory selection")
public class AiProviderFactorySelectionTest {

    private static Config empty() {
        return new Config(
                new Config.OpenAiConfig(null, "https://api.openai.com", "gpt-4o-mini"),
                new Config.AnthropicConfig(null, "claude-sonnet-4-0"),
                new Config.GoogleConfig(null, "gemini-2.0-flash"),
                new Config.DeepseekConfig(null),
                new Config.OllamaConfig(null, "http://localhost:11434"),
                0.1,
                30);
    }

    private static Config onlyAnthropic() {
        Config base = empty();
        return new Config(base.openai(), new Config.AnthropicConfig("key", "claude-sonnet-4-0"),
                base.google(), base.deepseek(), base.ollama(), 0.1, 30);
    }

    @Test
    @DisplayName("selection follows priority order")
    void selection_followsPriority() {
        Config openai = new Config(
                new Config.OpenAiConfig("key", "https://api.openai.com", "gpt-4o-mini"),
                new Config.AnthropicConfig("key", "claude-sonnet-4-0"),
                empty().google(), empty().deepseek(), empty().ollama(), 0.1, 30);

        assertThat(AiProviderFactory.selectedProvider(openai)).isEqualTo("openai");
        assertThat(AiProviderFactory.selectedProvider(onlyAnthropic())).isEqualTo("anthropic");
    }

    @Test
    @DisplayName("selection fails when nothing is configured")
    void selection_failsWhenEmpty() {
        assertThatThrownBy(() -> AiProviderFactory.selectedProvider(empty()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No AI provider configured");
    }

    @Test
    @DisplayName("forced provider creates matching model")
    void forced_createsMatching() {
        ChatModel model = AiProviderFactory.createChatModel(onlyAnthropic(), "ANTHROPIC");

        assertThat(model).isInstanceOf(AnthropicChatModel.class);
    }

    @Test
    @DisplayName("forced provider requires configuration")
    void forced_requiresConfiguration() {
        assertThatThrownBy(() -> AiProviderFactory.createChatModel(empty(), "openai"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OPENAI_API_KEY");
        assertThatThrownBy(() -> AiProviderFactory.createChatModel(empty(), "ollama"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OLLAMA_MODEL");
    }

    @Test
    @DisplayName("unknown and blank forced providers are rejected")
    void forced_rejectsUnknown() {
        assertThatThrownBy(() -> AiProviderFactory.createChatModel(onlyAnthropic(), "cohere"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cohere");
        assertThatThrownBy(() -> AiProviderFactory.createChatModel(onlyAnthropic(), "   "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AiProviderFactory.createChatModel(onlyAnthropic(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("forced google, deepseek, and ollama create matching models")
    void forcedOthers_createMatching() {
        Config base = empty();
        Config google = new Config(base.openai(), base.anthropic(),
                new Config.GoogleConfig("key", "gemini-2.0-flash"), base.deepseek(), base.ollama(), 0.1, 30);
        Config deepseek = new Config(base.openai(), base.anthropic(), base.google(),
                new Config.DeepseekConfig("key"), base.ollama(), 0.1, 30);
        Config ollama = new Config(base.openai(), base.anthropic(), base.google(), base.deepseek(),
                new Config.OllamaConfig("llama3", "http://localhost:11434"), 0.1, 30);

        assertThat(AiProviderFactory.createChatModel(google, "google"))
                .isInstanceOf(GoogleGenAiChatModel.class);
        assertThat(AiProviderFactory.createChatModel(deepseek, "deepseek"))
                .isInstanceOf(DeepSeekChatModel.class);
        assertThat(AiProviderFactory.createChatModel(ollama, "ollama"))
                .isInstanceOf(OllamaChatModel.class);
        assertThat(AiProviderFactory.createChatModel(
                new Config(new Config.OpenAiConfig("k", "https://api.openai.com", "m"),
                        base.anthropic(), base.google(), base.deepseek(), base.ollama(), 0.1, 30),
                "openai")).isInstanceOf(OpenAiChatModel.class);
    }
}
