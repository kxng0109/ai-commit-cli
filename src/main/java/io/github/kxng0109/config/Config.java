package io.github.kxng0109.config;

/**
 * The {@code Config} record encapsulates configuration settings for multiple services,
 * including OpenAI, Google, and Ollama, along with additional parameters for AI behavior
 * and command execution.
 *
 * <p>This class provides a mechanism to load the configuration settings from environment variables,
 * allowing customization of service APIs and operational parameters through external properties.
 * Default values are used where environment variables are not set or invalid.</p>
 *
 * @param openai                the {@link OpenAiConfig} containing OpenAI-related API settings.
 * @param google                the {@link GoogleConfig} containing Google-related API settings.
 * @param ollama                the {@link OllamaConfig} containing Ollama service configuration.
 * @param temperature           the temperature value as a double used for AI response variability.
 * @param commandTimeoutSeconds the timeout value (in seconds) for command execution.
 */
public record Config(
        OpenAiConfig openai,
        GoogleConfig google,
        OllamaConfig ollama,
        double temperature,
        int commandTimeoutSeconds
) {
    /**
     * Loads configuration settings for multiple services (OpenAI, Google, and Ollama)
     * from environment variables. If specific environment variables are not set,
     * default values are applied for certain configurations.
     * <p>
     * The method creates instances of {@link OpenAiConfig}, {@link GoogleConfig}, and {@link OllamaConfig}
     * using the relevant environment variables (or defaults). It also sets a temperature for AI-related
     * operations and a timeout duration for execution commands.
     *
     * @return a {@link Config} object encapsulating environment-based configuration settings,
     * including API keys, base URLs, models, temperature, and timeout values.
     */
    public static Config loadFromEnv() {
        OpenAiConfig openai = new OpenAiConfig(
                System.getenv("OPENAI_API_KEY"),
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
     * Retrieves the value of an environment variable by its key. If the specified
     * environment variable is not set or contains a blank value, a default value is returned.
     *
     * @param key          the name of the environment variable to retrieve.
     * @param defaultValue the value to return if the environment variable is not set
     *                     or contains a blank value.
     * @return the value of the environment variable if set and non-blank; otherwise,
     * the specified default value.
     */
    private static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    /**
     * Parses the given string into a double value. If the parsing fails due to a {@code NumberFormatException},
     * or if the parsed value is not within the range [0.0, 1.0], a default value of {@code 0.1} is returned.
     *
     * @param value the string to be parsed into a double.
     * @return the parsed double value between 0.0 and 1.0 (inclusive), or 0.1 if the input is invalid or out of range.
     */
    private static double parseDouble(String value) {
        try {
            double parsed = Double.parseDouble(value);
            return (parsed < 0.0 || parsed > 1.0) ? 0.1 : parsed;
        } catch (NumberFormatException e) {
            return 0.1;
        }
    }

    /**
     * Parses the given string into an integer value. If the parsing fails due to a {@code NumberFormatException},
     * or if the parsed integer is not within the range (1 to 3599), a default value of {@code 30} is returned.
     *
     * @param value the string to be parsed into an integer.
     * @return the parsed integer value between 1 and 3599 (inclusive), or 30 if the input is invalid or out of range.
     */
    private static int parseInt(String value) {
        try {
            int i = Integer.parseInt(value);
            return (i > 0 && i < 3600) ? i : 30;
        } catch (NumberFormatException e) {
            return 30;
        }
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
     * Represents the configuration settings required to interact with Google services.
     * This record encapsulates the API key and the default model used for Google-related operations.
     *
     * @param apiKey the API key used to authenticate requests to Google APIs.
     * @param model  the name of the default model to use within Google services.
     */
    public record GoogleConfig(String apiKey, String model) {
        public boolean isConfigured() {
            return apiKey != null && !apiKey.isBlank();
        }
    }

    /**
     * Represents the configuration for the Ollama service.
     * This record encapsulates the model name and base URL required
     * to interact with the Ollama API.
     *
     * @param model   the name of the Ollama model to use for requests.
     * @param baseUrl the base URL of the Ollama API endpoint.
     */
    public record OllamaConfig(String model, String baseUrl) {
        public boolean isConfigured() {
            return model != null && !model.isBlank();
        }
    }
}
