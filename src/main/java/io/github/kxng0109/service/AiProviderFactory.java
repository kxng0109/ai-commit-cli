package io.github.kxng0109.service;

import com.google.genai.Client;
import io.github.kxng0109.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
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
 * Factory class for creating instances of {@code ChatModel} for various AI providers
 * including OpenAI, Google Generative AI (Gemini), and Ollama. The factory dynamically selects
 * the appropriate provider based on the provided configuration and initializes the respective model.
 * <p>
 * This class is designed as a utility class and cannot be instantiated.
 * </p>
 *
 * <h2>Supported AI Providers</h2>
 * <ul>
 *     <li>OpenAI / OpenAI-compatible APIs</li>
 *     <li>Google Generative AI (Gemini)</li>
 *     <li>Ollama (local or hosted)</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * Use the {@link #createChatModel(Config)} method to obtain an instance
 * of a configured {@code ChatModel} based on the provided {@code Config} object.
 * Ensure that the configuration specifies at least one valid AI provider.
 *
 * <h3>Exceptions</h3>
 * If no AI provider is configured, an {@code IllegalStateException} is thrown
 * with detailed guidance for setting up the supported providers.
 *
 * <h2>Thread Safety</h2>
 * This class is thread-safe as it contains only static methods and immutable configuration logic.
 *
 * @see Config
 * @see ChatModel
 */
public class AiProviderFactory {

    private static final Logger log = LoggerFactory.getLogger(AiProviderFactory.class);

    private AiProviderFactory() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Creates a {@code ChatModel} based on the configuration provided.
     * The method dynamically determines the appropriate AI provider (OpenAI, Google Gemini, or Ollama)
     * from the {@code Config} object and initializes the respective {@code ChatModel}.
     * If no provider is configured, it throws an {@code IllegalStateException}.
     *
     * @param config the {@code Config} object containing details such as API keys, base URLs,
     *               model names, and optional settings to integrate with supported AI providers.
     * @return a fully initialized {@code ChatModel} for the configured provider.
     * @throws IllegalStateException if no AI provider is configured in the {@code Config} object.
     */
    public static ChatModel createChatModel(Config config) {
        if (config.openai().isConfigured()) {
            log.info("Using OpenAI-compatible provider");
            log.debug("Base URL: {}. Model: {}", config.openai().baseUrl(), config.openai().model());
            return createOpenAiModel(config);
        }

        if (config.google().isConfigured()) {
            log.info("Using Google Gemini provider");
            log.debug("Model: {}", config.google().model());
            return createGoogleModel(config);
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
                        
                                    2. Google Gemini:
                                       export GOOGLE_API_KEY="your-api-key"
                                       export GOOGLE_MODEL="gemini-2.0-flash-exp"  # optional
                        
                                    3. Ollama (local):
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
