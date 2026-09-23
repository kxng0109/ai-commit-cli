package io.github.kxng0109.service;

import io.github.kxng0109.config.Config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;

/**
 * Guards for provider selection, output caps, and timeouts in {@link AiProviderFactory}.
 */
@DisplayName("AiProviderFactory reliability")
public class AiProviderFactoryReliabilityTest {

    @Test
    @DisplayName("anthropic-only config selects anthropic with output cap")
    void anthropicOnly_selectsAnthropicWithCap() {
        ChatModel model = AiProviderFactory.createChatModel(configWithAnthropic());

        assertThat(model).isInstanceOf(AnthropicChatModel.class);
        assertThat(model.getDefaultOptions().getMaxTokens()).isEqualTo(512);
        assertThat(model.getDefaultOptions().getModel()).isEqualTo("claude-sonnet-4-0");
    }

    @Test
    @DisplayName("google-only config selects google with output cap")
    void googleOnly_selectsGoogleWithCap() {
        ChatModel model = AiProviderFactory.createChatModel(configWithGoogle());

        assertThat(model).isInstanceOf(GoogleGenAiChatModel.class);
        GoogleGenAiChatOptions options = (GoogleGenAiChatOptions) model.getDefaultOptions();
        assertThat(options.getMaxOutputTokens()).isEqualTo(512);
        assertThat(options.getModel()).isEqualTo("gemini-2.0-flash");
    }

    @Test
    @DisplayName("deepseek-only config selects deepseek with output cap")
    void deepseekOnly_selectsDeepseekWithCap() {
        ChatModel model = AiProviderFactory.createChatModel(configWithDeepseek());

        assertThat(model).isInstanceOf(DeepSeekChatModel.class);
        assertThat(model.getDefaultOptions().getMaxTokens()).isEqualTo(512);
    }

    @Test
    @DisplayName("ollama-only config selects ollama with native prediction cap")
    void ollamaOnly_selectsOllamaWithCap() {
        ChatModel model = AiProviderFactory.createChatModel(configWithOllama());

        assertThat(model).isInstanceOf(OllamaChatModel.class);
        OllamaChatOptions options = (OllamaChatOptions) model.getDefaultOptions();
        assertThat(options.getNumPredict()).isEqualTo(512);
        assertThat(options.getModel()).isEqualTo("llama3");
    }

    @Test
    @DisplayName("openai config carries output cap and model")
    void openai_carriesCapAndModel() {
        Config config = new Config(
                new Config.OpenAiConfig("test-key", "https://api.openai.com", "gpt-4o-mini"),
                new Config.AnthropicConfig(null, "claude-sonnet-4-0"),
                new Config.GoogleConfig(null, "gemini-2.0-flash"),
                new Config.DeepseekConfig(null),
                new Config.OllamaConfig(null, "http://localhost:11434"),
                0.1,
                30);

        ChatModel model = AiProviderFactory.createChatModel(config);

        assertThat(model).isInstanceOf(OpenAiChatModel.class);
        assertThat(model.getDefaultOptions().getMaxTokens()).isEqualTo(512);
        assertThat(model.getDefaultOptions().getTemperature()).isEqualTo(0.1);
    }

    @Test
    @DisplayName("anthropic outranks google")
    void anthropic_outranksGoogle() {
        Config config = new Config(
                new Config.OpenAiConfig(null, "https://api.openai.com", "gpt-4o-mini"),
                new Config.AnthropicConfig("anthropic-key", "claude-sonnet-4-0"),
                new Config.GoogleConfig("google-key", "gemini-2.0-flash"),
                new Config.DeepseekConfig(null),
                new Config.OllamaConfig(null, "http://localhost:11434"),
                0.1,
                30);

        assertThat(AiProviderFactory.createChatModel(config)).isInstanceOf(AnthropicChatModel.class);
    }

    private static Config configWithAnthropic() {
        return new Config(
                new Config.OpenAiConfig(null, "https://api.openai.com", "gpt-4o-mini"),
                new Config.AnthropicConfig("anthropic-key", "claude-sonnet-4-0"),
                new Config.GoogleConfig(null, "gemini-2.0-flash"),
                new Config.DeepseekConfig(null),
                new Config.OllamaConfig(null, "http://localhost:11434"),
                0.2,
                30);
    }

    private static Config configWithGoogle() {
        return new Config(
                new Config.OpenAiConfig(null, "https://api.openai.com", "gpt-4o-mini"),
                new Config.AnthropicConfig(null, "claude-sonnet-4-0"),
                new Config.GoogleConfig("google-key", "gemini-2.0-flash"),
                new Config.DeepseekConfig(null),
                new Config.OllamaConfig(null, "http://localhost:11434"),
                0.2,
                30);
    }

    private static Config configWithDeepseek() {
        return new Config(
                new Config.OpenAiConfig(null, "https://api.openai.com", "gpt-4o-mini"),
                new Config.AnthropicConfig(null, "claude-sonnet-4-0"),
                new Config.GoogleConfig(null, "gemini-2.0-flash"),
                new Config.DeepseekConfig("deepseek-key"),
                new Config.OllamaConfig(null, "http://localhost:11434"),
                0.2,
                30);
    }

    private static Config configWithOllama() {
        return new Config(
                new Config.OpenAiConfig(null, "https://api.openai.com", "gpt-4o-mini"),
                new Config.AnthropicConfig(null, "claude-sonnet-4-0"),
                new Config.GoogleConfig(null, "gemini-2.0-flash"),
                new Config.DeepseekConfig(null),
                new Config.OllamaConfig("llama3", "http://localhost:11434"),
                0.2,
                30);
    }
}
