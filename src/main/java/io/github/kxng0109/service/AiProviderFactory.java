package io.github.kxng0109.service;

import com.google.genai.Client;
import io.github.kxng0109.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
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
 * A factory class for creating instances of {@code ChatClient} based on the configured AI provider.
 * This class supports multiple AI providers including OpenAI, Google Gemini, and Ollama. It ensures
 * proper initialization of the client using the settings provided in the {@code Config} object.
 *
 * This class cannot be instantiated as it is designed to provide static factory methods.
 */
public class AiProviderFactory {

    private static final Logger log = LoggerFactory.getLogger(AiProviderFactory.class);

    private AiProviderFactory() {
    }

    /**
     * Creates a {@code ChatClient} instance based on the provided configuration.
     * The method selects and configures an appropriate AI provider (OpenAI, Google, or Ollama)
     * depending on the information present in the {@code Config} object.
     *
     * @param config the configuration object containing details for integrating with the AI provider,
     *               such as API key, base URL, model, and other necessary settings
     * @return a fully initialized {@code ChatClient} for the specified AI provider if configured;
     *         otherwise, throws an {@code IllegalStateException} if no provider is configured
     */
    public static ChatClient createChatClient(Config config) {
        if (config.openai().isConfigured()) {
            log.info("OpenAI Chat Client is configured. Using OpenAI-compatible provider");
            log.debug("Base URL: {}. Model: {}", config.openai().baseUrl(), config.openai().model());
            return createOpenAiClient(config);
        }

        if (config.google().isConfigured()) {
            log.info("Google Chat Client is configured. Using Google Gemini provider");
            log.debug("Model: {}", config.google().model());
            return createGoogleClient(config);
        }

        if (config.ollama().isConfigured()) {
            log.info("Ollama is configured, Using Ollama provider.");
            log.debug("Base URL: {}. Model: {}", config.ollama().baseUrl(), config.ollama().model());
            return createOllamaClient(config);
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
                        
                                    Note: Use "set" for command prompt and "$env:" for PowerShell instead of "export" if you are using windows
                                    Run 'ai-commit --help' for more information.
                        """
        );
    }

    /**
     * Creates a {@code ChatClient} instance configured to interact with the OpenAI API.
     * Uses settings from the provided {@code Config} object, including API key, base URL,
     * model, and temperature.
     *
     * @param config the configuration object containing all the necessary details to
     *               integrate with the OpenAI API, such as API key, base URL, model, and temperature
     * @return a fully configured {@code ChatClient} for interacting with the OpenAI API
     */
    private static ChatClient createOpenAiClient(Config config) {
        OpenAiApi api = OpenAiApi.builder()
                                 .apiKey(config.openai().apiKey())
                                 .baseUrl(config.openai().baseUrl())
                                 .responseErrorHandler(new DefaultResponseErrorHandler())
                                 .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                                                     .model(config.openai().model())
                                                     .temperature(config.temperature())
                                                     .build();

        OpenAiChatModel model = OpenAiChatModel.builder()
                                               .openAiApi(api)
                                               .defaultOptions(options)
                                               .build();

        return ChatClient.builder(model).build();
    }

    /**
     * Creates a {@code ChatClient} instance configured to interact with Google's AI services.
     * Uses settings from the provided {@code Config} object, including API key, model,
     * and temperature configuration.
     *
     * @param config the configuration object containing the necessary details to integrate with
     *               Google's AI services, such as API key, model, and temperature settings
     * @return a fully configured {@code ChatClient} for interacting with Google's AI services
     */
    private static ChatClient createGoogleClient(Config config) {
        Client client = Client.builder()
                              .apiKey(config.openai().apiKey())
                              .build();

        GoogleGenAiChatOptions options = GoogleGenAiChatOptions.builder()
                                                               .temperature(config.temperature())
                                                               .model(config.openai().model())
                                                               .build();

        GoogleGenAiChatModel model = GoogleGenAiChatModel.builder()
                                                         .defaultOptions(options)
                                                         .genAiClient(client)
                                                         .build();

        return ChatClient.builder(model).build();
    }

    /**
     * Creates a {@code ChatClient} instance configured to interact with Ollama's AI services.
     * This method uses settings from the provided {@code Config} object, including the base URL,
     * model name, and temperature to properly initialize the client.
     *
     * @param config the configuration object containing the necessary details to integrate with
     *               Ollama's services, such as the base URL, model name, and temperature settings
     * @return a fully configured {@code ChatClient} for interacting with Ollama's AI services
     */
    private static ChatClient createOllamaClient(Config config) {
        OllamaApi api = OllamaApi.builder()
                .baseUrl(config.ollama().baseUrl())
                .responseErrorHandler(new DefaultResponseErrorHandler())
                .build();

        OllamaChatOptions options = OllamaChatOptions.builder()
                .model(config.ollama().model())
                .temperature(config.temperature())
                .build();

        OllamaChatModel model = OllamaChatModel.builder()
                .ollamaApi(api)
                .defaultOptions(options)
                .build();

        return ChatClient.builder(model).build();
    }
}
