package io.github.kxng0109.service;

import com.google.genai.Client;
import io.github.kxng0109.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.web.client.DefaultResponseErrorHandler;

/**
 * Factory class for creating instances of {@code ChatModel} based on configuration.
 * This utility provides functionality to automatically select and initialize the
 * appropriate AI provider (e.g., OpenAI, Anthropic, Google, Deepseek, or Ollama)
 * based on the provided {@code Config}.
 *
 * <p>This class is designed as a utility and cannot be instantiated, ensuring
 * adherence to the singleton-like design for shared logic.</p>
 *
 * <ul>
 * Key Features:
 * <li>Supports multiple AI providers with seamless integration.</li>
 * <li>Ensures AI model configuration based on the primary detected provider.</li>
 * <li>Throws proper exceptions for missing or invalid configurations.</li>
 * </ul>
 *
 * <p>Example Usage:</p>
 * <pre>{@code
 * Config config = ...; // Set up with API keys, base URLs, and models
 * ChatModel chatModel = AiProviderFactory.createChatModel(config);
 * }</pre>
 *
 * <p>This factory handles all provider-specific details, offering a unified method
 * for accessing AI chat functionalities without needing to manually implement provider logic.</p>
 */
public class AiProviderFactory {

    private static final Logger log = LoggerFactory.getLogger(AiProviderFactory.class);

    private AiProviderFactory() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Creates a {@code ChatModel} instance based on the provided configuration.
     * This method automatically selects the appropriate AI provider (e.g., OpenAI, Anthropic, Google, Deepseek, or Ollama)
     * based on the configured settings, initializes the provider-specific model, and returns a fully configured
     * {@code ChatModel} instance.
     *
     * If no AI provider is configured, an {@code IllegalStateException} is thrown with guidance
     * on setting up the configuration.
     *
     * @param config the configuration object containing necessary details for integrating with various
     *               AI providers, such as API keys, base URLs, model names, and other provider-specific settings.
     * @return a fully configured {@code ChatModel} instance corresponding to the first detected, properly
     *         configured AI provider.
     * @throws IllegalStateException if no AI provider is configured.
     */
    public static ChatModel createChatModel(Config config) {
        if (config.openai().isConfigured()) {
            log.info("Using OpenAI-compatible provider");
            log.debug("Base URL: {}. Model: {}", config.openai().baseUrl(), config.openai().model());
            return createOpenAiModel(config);
        }

        if (config.anthropic().isConfigured()) {
            log.info("Using Anthropic provider");
            log.debug("Model: {}", config.anthropic().model());
            return createAnthropicModel(config);
        }

        if (config.google().isConfigured()) {
            log.info("Using Google Gemini provider");
            log.debug("Model: {}", config.google().model());
            return createGoogleModel(config);
        }

        if (config.deepseek().isConfigured()) {
            log.info("Using Deepseek provider");
            return createDeepseekModel(config);
        }

        if (config.ollama().isConfigured()) {
            log.info("Ollama is configured, Using Ollama provider.");
            log.debug("Base URL: {}. Model: {}", config.ollama().baseUrl(), config.ollama().model());
            return createOllamaModel(config);
        }

        throw new IllegalStateException(
                """
                        No AI provider configured. Please set one of the following:
                        
                                    1. OpenAI / OpenAI-compatible:
                                       export OPENAI_API_KEY="your-api-key"
                                       export OPENAI_MODEL="gpt-4o"  # optional
                                       export OPENAI_BASE_URL="https://api.openai.com"  # optional
                        
                                    2. Anthropic Claude:
                                       export ANTHROPIC_API_KEY="your-api-key"
                                       export ANTHROPIC_MODEL="claude-sonnet-4-0"  # optional
                        
                                    3. Google Gemini:
                                       export GOOGLE_API_KEY="your-api-key"
                                       export GOOGLE_MODEL="gemini-2.0-flash-exp"  # optional
                        
                                    4. Deepseek:
                                       export DEEPSEEK_API_KEY="your-api-key"
                        
                                    5. Ollama (local):
                                       export OLLAMA_MODEL="llama3"
                                       export OLLAMA_BASE_URL="http://localhost:11434"  # optional
                        
                                    Run 'ai-commit --help' for more information.
                        """
        );
    }

    /**
     * Creates a {@code ChatModel} instance specifically configured to interact with the OpenAI API.
     * This method initializes the necessary API client and sets up default chat options, such as
     * the model and temperature, based on the provided {@code Config} object.
     *
     * @param config the configuration object containing the required details for integrating with
     *               the OpenAI API, including API key, base URL, model name, and temperature settings
     * @return a fully configured {@code ChatModel} for interacting with the OpenAI API
     */
    private static ChatModel createOpenAiModel(Config config) {
        OpenAiApi api = OpenAiApi.builder()
                                 .apiKey(config.openai().apiKey())
                                 .baseUrl(config.openai().baseUrl())
                                 .responseErrorHandler(new DefaultResponseErrorHandler())
                                 .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                                                     .model(config.openai().model())
                                                     .temperature(config.temperature())
                                                     .build();

        return OpenAiChatModel.builder()
                              .openAiApi(api)
                              .defaultOptions(options)
                              .build();
    }

    /**
     * Creates a {@code ChatModel} instance specifically configured to interact with the Google Generative AI API.
     * This method initializes the required client and sets up chat options such as temperature and model,
     * based on the provided {@code Config} object.
     *
     * @param config the configuration object containing the necessary details for integrating with the
     *               Google Generative AI API, including API key, model, and temperature settings
     * @return a fully configured {@code ChatModel} for interacting with the Google Generative AI API
     */
    private static ChatModel createGoogleModel(Config config) {
        Client client = Client.builder()
                              .apiKey(config.google().apiKey())
                              .build();

        GoogleGenAiChatOptions options = GoogleGenAiChatOptions.builder()
                                                               .temperature(config.temperature())
                                                               .model(config.google().model())
                                                               .build();

        return GoogleGenAiChatModel.builder()
                                   .defaultOptions(options)
                                   .genAiClient(client)
                                   .build();
    }

    /**
     * Creates a {@code ChatModel} instance specifically configured to interact with the Anthropic API.
     * This method initializes the required {@code AnthropicApi} client and sets up default chat options,
     * including the model and temperature, based on the provided {@code Config} object.
     *
     * @param config the configuration object containing the required details for integrating with
     *               the Anthropic API, such as API key, model name, and temperature settings
     * @return a fully configured {@code ChatModel} for interacting with the Anthropic API
     */
    private static ChatModel createAnthropicModel(Config config) {
        AnthropicApi api = AnthropicApi.builder()
                                       .apiKey(config.anthropic().apiKey())
                                       .responseErrorHandler(new DefaultResponseErrorHandler())
                                       .build();

        AnthropicChatOptions options = AnthropicChatOptions.builder()
                                                           .model(config.anthropic().model())
                                                           .temperature(config.temperature())
                                                           .build();

        return AnthropicChatModel.builder()
                                 .defaultOptions(options)
                                 .anthropicApi(api)
                                 .build();
    }

    /**
     * Creates a {@code ChatModel} instance specifically configured to interact with the DeepSeek API.
     * This method initializes the required DeepSeek API client and sets up default chat options,
     * including the temperature, based on the provided {@code Config} object.
     *
     * @param config the configuration object containing the necessary details for integrating
     *               with the DeepSeek API, such as API key and temperature settings.
     * @return a fully configured {@code ChatModel} for interacting with the DeepSeek API.
     */
    private static ChatModel createDeepseekModel(Config config) {
        DeepSeekApi api = DeepSeekApi.builder()
                                     .apiKey(config.deepseek().apiKey())
                                     .responseErrorHandler(new DefaultResponseErrorHandler())
                                     .build();

        DeepSeekChatOptions options = DeepSeekChatOptions.builder()
                                                         .temperature(config.temperature())
                                                         .build();

        return DeepSeekChatModel.builder()
                                .deepSeekApi(api)
                                .defaultOptions(options)
                                .build();
    }

    /**
     * Creates a {@code ChatModel} instance specifically configured to interact with the Ollama API.
     * Utilizes the provided {@code Config} object to set up the necessary API client, chat options,
     * and other configuration details such as base URL, model, and temperature settings.
     *
     * @param config the configuration object containing the required details
     *               to integrate with the Ollama API, including base URL,
     *               model name, and temperature settings
     * @return a fully configured {@code ChatModel} for interacting with the Ollama API
     */
    private static ChatModel createOllamaModel(Config config) {
        OllamaApi api = OllamaApi.builder()
                                 .baseUrl(config.ollama().baseUrl())
                                 .responseErrorHandler(new DefaultResponseErrorHandler())
                                 .build();

        OllamaChatOptions options = OllamaChatOptions.builder()
                                                     .model(config.ollama().model())
                                                     .temperature(config.temperature())
                                                     .build();

        return OllamaChatModel.builder()
                              .ollamaApi(api)
                              .defaultOptions(options)
                              .build();
    }
}
