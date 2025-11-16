package io.github.kxng0109.service;

import io.github.kxng0109.config.Config;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;

import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

public class AiProviderFactoryTest {

    @Test
    void createChatModel_withNoProviderConfigured_shouldThrowException() {
        Config config = new Config(
                new Config.OpenAiConfig(null, "https://api.openai.com", "gpt-4o-mini"),
                new Config.AnthropicConfig(null, "claude-sonnet-4-0"),
                new Config.GoogleConfig(null, "gemini-2.0-flash"),
                new Config.DeepseekConfig(null),
                new Config.OllamaConfig(null, "http://localhost:11434"),
                0.1,
                30
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> AiProviderFactory.createChatModel(config)
        );

        assertTrue(exception.getMessage().contains("No AI provider configured"));
    }

    @Test
    void createChatModel_withOpenAiConfigured_shouldReturnChatModel() {
        Config config = new Config(
                new Config.OpenAiConfig("test-key", "https://api.openai.com", "gpt-4o-mini"),
                new Config.AnthropicConfig(null, "claude-sonnet-4-0"),
                new Config.GoogleConfig(null, "gemini-2.0-flash"),
                new Config.DeepseekConfig(null),
                new Config.OllamaConfig(null, "http://localhost:11434"),
                0.1,
                30
        );

        ChatModel chatModel = AiProviderFactory.createChatModel(config);

        assertNotNull(chatModel);
        assertInstanceOf(OpenAiChatModel.class, chatModel);
    }

    @Test
    void createChatModel_withMultipleProviders_shouldPrioritizeOpenAi() {
        Config config = new Config(
                new Config.OpenAiConfig("openai-key", "https://api.openai.com", "gpt-4o-mini"),
                new Config.AnthropicConfig("anthropic-key", "claude-sonnet-4-0"),
                new Config.GoogleConfig("google-key", "gemini-2.0-flash"),
                new Config.DeepseekConfig("deepseek-key"),
                new Config.OllamaConfig("llama3", "http://localhost:11434"),
                0.1,
                30
        );

        ChatModel chatModel = AiProviderFactory.createChatModel(config);

        assertNotNull(chatModel);
        assertInstanceOf(OpenAiChatModel.class, chatModel);
    }

    @Test
    void constructor_shouldThrowUnsupportedOperationException() {
        InvocationTargetException exception = assertThrows(
                InvocationTargetException.class,
                () -> {
                    var constructor = AiProviderFactory.class.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    constructor.newInstance();
                }
        );

        assertInstanceOf(UnsupportedOperationException.class, exception.getCause());
        assertEquals("Utility class", exception.getCause().getMessage());
    }
}
