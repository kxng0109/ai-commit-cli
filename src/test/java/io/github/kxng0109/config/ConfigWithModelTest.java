package io.github.kxng0109.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards for per-run model overrides in {@link Config}.
 */
@DisplayName("Config model override")
public class ConfigWithModelTest {

    private static Config base() {
        return new Config(
                new Config.OpenAiConfig("key", "https://api.openai.com", "gpt-4o-mini"),
                new Config.AnthropicConfig(null, "claude-sonnet-4-0"),
                new Config.GoogleConfig(null, "gemini-2.0-flash"),
                new Config.DeepseekConfig(null),
                new Config.OllamaConfig(null, "http://localhost:11434"),
                0.1,
                30);
    }

    @Test
    @DisplayName("openai model is replaced, rest preserved")
    void openai_replaced() {
        Config updated = base().withModel("openai", "gpt-4o");

        assertThat(updated.openai().model()).isEqualTo("gpt-4o");
        assertThat(updated.openai().apiKey()).isEqualTo("key");
        assertThat(updated.anthropic().model()).isEqualTo("claude-sonnet-4-0");
        assertThat(updated.temperature()).isEqualTo(0.1);
    }

    @Test
    @DisplayName("anthropic, google, and ollama models are replaced")
    void others_replaced() {
        assertThat(base().withModel("ANTHROPIC", "claude-x").anthropic().model()).isEqualTo("claude-x");
        assertThat(base().withModel(" Google ", "gemini-x").google().model()).isEqualTo("gemini-x");
        assertThat(base().withModel("ollama", "llama3").ollama().model()).isEqualTo("llama3");
    }

    @Test
    @DisplayName("model value is trimmed")
    void model_trimmed() {
        assertThat(base().withModel("openai", "  gpt-4o  ").openai().model()).isEqualTo("gpt-4o");
    }

    @Test
    @DisplayName("deepseek rejects model overrides")
    void deepseek_rejected() {
        assertThatThrownBy(() -> base().withModel("deepseek", "deepseek-reasoner"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fixed model");
    }

    @Test
    @DisplayName("unknown and blank providers are rejected")
    void unknownProviders_rejected() {
        assertThatThrownBy(() -> base().withModel("cohere", "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cohere");
        assertThatThrownBy(() -> base().withModel("   ", "x"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> base().withModel(null, "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("blank models are rejected")
    void blankModels_rejected() {
        assertThatThrownBy(() -> base().withModel("openai", "   "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> base().withModel("openai", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
