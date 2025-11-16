package io.github.kxng0109.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigTest {

    @Test
    void loadFromEnv_shouldLoadDefaultValues() {
        Config config = Config.loadFromEnv();

        assertNotNull(config);
        assertEquals(0.1, config.temperature());
        assertEquals(30, config.commandTimeoutSeconds());
    }

    @Test
    void openAiConfig_isConfigured_shouldReturnFalseWhenApiKeyIsNull() {
        Config.OpenAiConfig config = new Config.OpenAiConfig(null, "url", "model");

        assertFalse(config.isConfigured());
    }

    @Test
    void openAiConfig_isConfigured_shouldReturnFalseWhenApiKeyIsBlank() {
        Config.OpenAiConfig config = new Config.OpenAiConfig("  ", "url", "model");

        assertFalse(config.isConfigured());
    }

    @Test
    void openAiConfig_isConfigured_shouldReturnTrueWhenApiKeyIsSet() {
        Config.OpenAiConfig config = new Config.OpenAiConfig("sk-test", "url", "model");

        assertTrue(config.isConfigured());
    }

    @Test
    void anthropicConfig_isConfigured_shouldReturnFalseWhenApiKeyIsNull() {
        Config.AnthropicConfig config = new Config.AnthropicConfig(null, "model");

        assertFalse(config.isConfigured());
    }

    @Test
    void googleConfig_isConfigured_shouldReturnFalseWhenApiKeyIsNull() {
        Config.GoogleConfig config = new Config.GoogleConfig(null, "model");

        assertFalse(config.isConfigured());
    }

    @Test
    void deepseekConfig_isConfigured_shouldReturnFalseWhenApiKeyIsNull() {
        Config.DeepseekConfig config = new Config.DeepseekConfig(null);

        assertFalse(config.isConfigured());
    }

    @Test
    void ollamaConfig_isConfigured_shouldReturnFalseWhenModelIsNull() {
        Config.OllamaConfig config = new Config.OllamaConfig(null, "url");

        assertFalse(config.isConfigured());
    }

    @Test
    void ollamaConfig_isConfigured_shouldReturnTrueWhenModelIsSet() {
        Config.OllamaConfig config = new Config.OllamaConfig("llama3", "url");

        assertTrue(config.isConfigured());
    }
}
