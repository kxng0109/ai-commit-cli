package config;

/**
 * Encapsulates configuration details for integrating with OpenAI, Google, and Ollama services,
 * as well as AI-specific parameters such as temperature and command timeout duration.
 * Uses nested record types {@code OpenAiConfig}, {@code GoogleConfig}, and {@code OllamaConfig}
 * for modular representation of service-specific settings.
 * Provides functionality to load configurations from environment variables while supporting
 * default values when variables are not explicitly set.
 */
public record Config(
        OpenAiConfig openai,
        GoogleConfig google,
        OllamaConfig ollama,
        double temperature,
        int commandTimeoutSeconds
) {
    /**
     * Loads a {@code Config} object using environment variables to configure OpenAI, Google,
     * and Ollama services, along with AI parameters such as temperature and timeout duration.
     * If any required environment variables are not set, default values are used.
     *
     * @return a {@code Config} object initialized with the necessary service configurations
     * and parameters.
     */
    public static Config loadFromEnv() {
        OpenAiConfig openai = new OpenAiConfig(
                System.getenv("OPENAPI_API_KEY"),
                getEnvOrDefault("OPENAI_BASE_URL", "https://api.openai.com"),
                getEnvOrDefault("OPENAI_MODEL", "gpt-4o")
        );

        GoogleConfig google = new GoogleConfig(
                System.getenv("GOOGLE_API_KEY"),
                getEnvOrDefault("GOOGLE_MODEL", "gemini-pro")
        );

        OllamaConfig ollama = new OllamaConfig(
                System.getenv("OLLAMA_MODEL"),
                getEnvOrDefault("OLLAMA_BASE_URL", "http://localhost:11434")
        );

        double temperature = parseDouble(
                getEnvOrDefault("AI_TEMPERATURE", "0.1")
        );

        int timeout = parseInt(
                getEnvOrDefault("AI_TIMEOUT", "30")
        );

        return new Config(
                openai,
                google,
                ollama,
                temperature,
                timeout
        );
    }

    /**
     * Represents the configuration settings for OpenAI.
     * This configuration includes the API key, base URL, and model name
     * required to interact with the OpenAI API.
     *
     * @param apiKey  the API key used to authenticate requests to the OpenAI API
     * @param baseUrl the base URL of the OpenAI API
     * @param model   the default model to use for OpenAI requests
     */
    public record OpenAiConfig(String apiKey, String baseUrl, String model) {
        public boolean isConfigured() {
            return apiKey != null && !apiKey.isBlank();
        }
    }

    /**
     * Represents the configuration settings for Google's AI services.
     * This configuration includes the API key and model name required to interact
     * with Google's AI functionalities.
     *
     * @param apiKey the API key used to authenticate requests to Google's AI services
     * @param model  the default model to use for Google's AI requests
     */
    public record GoogleConfig(String apiKey, String model) {
        public boolean isConfigured() {
            return apiKey != null && !apiKey.isBlank();
        }
    }

    /**
     * Represents the configuration settings for Ollama services.
     * This configuration includes the model name and base URL required
     * to interact with Ollama's AI functionalities.
     *
     * @param model   the default model to use for Ollama requests
     * @param baseUrl the base URL of the Ollama service
     */
    public record OllamaConfig(String model, String baseUrl) {
        public boolean isConfigured() {
            return model != null && !model.isBlank();
        }
    }

    /**
     * Retrieves the value of an environment variable identified by the specified key.
     * If the environment variable is not set or is blank, returns the provided default value.
     *
     * @param key          the name of the environment variable to retrieve.
     * @param defaultValue the value to return if the environment variable is not set or is blank.
     * @return the value of the environment variable, or the default value if the environment variable is not set or is blank.
     */
    private static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    /**
     * Parses the given string into a double value. If the parsing fails due to a
     * {@code NumberFormatException}, a default value of 0.1 is returned.
     *
     * @param value the string to be parsed into a double.
     * @return the parsed double value, or 0.1 if the input string cannot be successfully parsed.
     */
    private static double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.1;
        }
    }

    /**
     * Parses the given string into an integer value. If the parsing fails due to
     * a {@code NumberFormatException}, a default value of 30 is returned.
     *
     * @param value the string to be parsed into an integer.
     * @return the parsed integer value, or 30 if the input string cannot be successfully parsed.
     */
    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 30;
        }
    }
}
