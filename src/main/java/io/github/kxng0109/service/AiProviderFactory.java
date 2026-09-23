package io.github.kxng0109.service;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
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
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Locale;

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

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration OLLAMA_READ_TIMEOUT = Duration.ofSeconds(120);
    private static final int GOOGLE_TIMEOUT_MILLIS = 60_000;
    private static final int MAX_OUTPUT_TOKENS = 512;

    private AiProviderFactory() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Builds a {@code RestClient.Builder} with bounded connect and read timeouts.
     * <p>
     * Uses {@code SimpleClientHttpRequestFactory} so no extra HTTP client dependency is needed.
     * </p>
     *
     * @param connect the connect timeout, must not be {@code null}
     * @param read the read timeout, must not be {@code null}
     * @return a configured builder
     */
    private static RestClient.Builder timedRestClient(Duration connect, Duration read) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connect);
        factory.setReadTimeout(read);
        return RestClient.builder().requestFactory(factory);
    }

    /**
     * Creates a {@code ChatModel} instance based on the provided configuration.
     * This method automatically selects the appropriate AI provider (e.g., OpenAI, Anthropic, Google, Deepseek, or Ollama)
     * based on the configured settings, initializes the provider-specific model, and returns a fully configured
     * {@code ChatModel} instance.
     * <p>
     * If no AI provider is configured, an {@code IllegalStateException} is thrown with guidance
     * on setting up the configuration.
     *
     * @param config the configuration object containing necessary details for integrating with various
     *               AI providers, such as API keys, base URLs, model names, and other provider-specific settings.
     * @return a fully configured {@code ChatModel} instance corresponding to the first detected, properly
     * configured AI provider.
     * @throws IllegalStateException if no AI provider is configured.
     */
    public static ChatModel createChatModel(Config config) {
        return createChatModel(config, selectedProvider(config));
    }

    /**
     * Creates a chat model for the named provider.
     *
     * @param config the application configuration containing provider settings and model parameters
     * @param provider the provider name (case-insensitive): openai, anthropic, google, deepseek, ollama
     * @return a {@code ChatModel} instance for the requested provider
     * @throws IllegalArgumentException when the provider name is unknown
     * @throws IllegalStateException when the requested provider is not configured
     */
    public static ChatModel createChatModel(Config config, String provider) {
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException(
                    "Unknown provider ''. Supported: openai, anthropic, google, deepseek, ollama.");
        }
        String name = provider.trim().toLowerCase(Locale.ROOT);
        switch (name) {
            case "openai":
                requireConfigured(config.openai().isConfigured(), "OpenAI", "OPENAI_API_KEY");
                log.info("Using OpenAI-compatible provider");
                log.debug("Base URL: {}. Model: {}", config.openai().baseUrl(), config.openai().model());
                return createOpenAiModel(config);
            case "anthropic":
                requireConfigured(config.anthropic().isConfigured(), "Anthropic", "ANTHROPIC_API_KEY");
                log.info("Using Anthropic provider");
                log.debug("Model: {}", config.anthropic().model());
                return createAnthropicModel(config);
            case "google":
                requireConfigured(config.google().isConfigured(), "Google", "GOOGLE_API_KEY");
                log.info("Using Google Gemini provider");
                log.debug("Model: {}", config.google().model());
                return createGoogleModel(config);
            case "deepseek":
                requireConfigured(config.deepseek().isConfigured(), "DeepSeek", "DEEPSEEK_API_KEY");
                log.info("Using Deepseek provider");
                return createDeepseekModel(config);
            case "ollama":
                requireConfigured(config.ollama().isConfigured(), "Ollama", "OLLAMA_MODEL");
                log.info("Ollama is configured, Using Ollama provider.");
                log.debug("Base URL: {}. Model: {}", config.ollama().baseUrl(), config.ollama().model());
                return createOllamaModel(config);
            default:
                throw new IllegalArgumentException(
                        "Unknown provider '" + provider.trim()
                                + "'. Supported: openai, anthropic, google, deepseek, ollama.");
        }
    }

    /**
     * Returns the first configured provider in priority order.
     *
     * @param config the application configuration, must not be {@code null}
     * @return the provider name: openai, anthropic, google, deepseek, or ollama
     * @throws IllegalStateException if no AI provider is configured
     */
    public static String selectedProvider(Config config) {
        if (config.openai().isConfigured()) {
            return "openai";
        }
        if (config.anthropic().isConfigured()) {
            return "anthropic";
        }
        if (config.google().isConfigured()) {
            return "google";
        }
        if (config.deepseek().isConfigured()) {
            return "deepseek";
        }
        if (config.ollama().isConfigured()) {
            return "ollama";
        }
        throw new IllegalStateException("No AI provider configured. Set at least one API key (e.g., OPENAI_API_KEY) or OLLAMA_MODEL.");
    }

    /**
     * Requires a provider to be configured, raising an actionable error otherwise.
     *
     * @param configured whether the provider is configured
     * @param displayName the human-readable provider name for messages
     * @param envVar the environment variable that configures the provider
     * @throws IllegalStateException when the provider is not configured
     */
    private static void requireConfigured(boolean configured, String displayName, String envVar) {
        if (!configured) {
            throw new IllegalStateException(
                    displayName + " provider selected but not configured. Set " + envVar + ".");
        }
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
                                  .restClientBuilder(timedRestClient(CONNECT_TIMEOUT, READ_TIMEOUT))
                                  .responseErrorHandler(new DefaultResponseErrorHandler())
                                  .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                                                      .model(config.openai().model())
                                                      .temperature(config.temperature())
                                                      .maxTokens(MAX_OUTPUT_TOKENS)
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
        HttpOptions httpOptions = HttpOptions.builder()
                                             .timeout(GOOGLE_TIMEOUT_MILLIS)
                                             .build();
        Client client = Client.builder()
                               .apiKey(config.google().apiKey())
                               .httpOptions(httpOptions)
                               .build();

        GoogleGenAiChatOptions options = GoogleGenAiChatOptions.builder()
                                                                .temperature(config.temperature())
                                                                .model(config.google().model())
                                                                .maxOutputTokens(MAX_OUTPUT_TOKENS)
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
                                        .restClientBuilder(timedRestClient(CONNECT_TIMEOUT, READ_TIMEOUT))
                                        .responseErrorHandler(new DefaultResponseErrorHandler())
                                        .build();

        AnthropicChatOptions options = AnthropicChatOptions.builder()
                                                            .model(config.anthropic().model())
                                                            .temperature(config.temperature())
                                                            .maxTokens(MAX_OUTPUT_TOKENS)
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
                                      .restClientBuilder(timedRestClient(CONNECT_TIMEOUT, READ_TIMEOUT))
                                      .responseErrorHandler(new DefaultResponseErrorHandler())
                                      .build();

        DeepSeekChatOptions options = DeepSeekChatOptions.builder()
                                                          .model("deepseek-chat")
                                                          .temperature(config.temperature())
                                                          .maxTokens(MAX_OUTPUT_TOKENS)
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
                                  .restClientBuilder(timedRestClient(CONNECT_TIMEOUT, OLLAMA_READ_TIMEOUT))
                                  .responseErrorHandler(new DefaultResponseErrorHandler())
                                  .build();

        OllamaChatOptions options = OllamaChatOptions.builder()
                                                      .model(config.ollama().model())
                                                      .temperature(config.temperature())
                                                      .numPredict(MAX_OUTPUT_TOKENS)
                                                      .build();

        return OllamaChatModel.builder()
                              .ollamaApi(api)
                              .defaultOptions(options)
                              .build();
    }
}
