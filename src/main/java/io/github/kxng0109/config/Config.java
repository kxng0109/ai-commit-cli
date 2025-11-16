package io.github.kxng0109.config;

/**
 * Represents the main configuration object for an application integrating multiple AI services.
 * This configuration class aggregates settings for OpenAI, Anthropic, Google, Deepseek, Ollama,
 * along with general settings such as AI temperature and command timeout.
 *
 * <p>Instances of this record are typically constructed using the {@code loadFromEnv()} method,
 * which reads the necessary settings from environment variables and applies defaults where needed.</p>
 *
 * @param openai              configuration for OpenAI services, including API key, base URL, and model.
 * @param anthropic           configuration for Anthropic services, including API key and model.
 * @param google              configuration for Google services, including API key and model.
 * @param deepseek            configuration for Deepseek services, including the API key.
 * @param ollama              configuration for Ollama services, including model and base URL.
 * @param temperature         a double representing the AI's temperature (creativity level) ranging
 *                             from 0.0 (deterministic) to 1.0 (high creativity).
 * @param commandTimeoutSeconds an integer specifying the timeout (in seconds) for AI command executions,
 *                              within a valid range of 1 to 3599 seconds.
 */
public record Config(
        OpenAiConfig openai,
        AnthropicConfig anthropic,
        GoogleConfig google,
        DeepseekConfig deepseek,
        OllamaConfig ollama,
        double temperature,
        int commandTimeoutSeconds
) {
    /**
     * Loads and constructs a {@link Config} object using the environment variables.
     * This method retrieves configuration data for various APIs (OpenAI, Anthropic, Google,
     * Deepseek, Ollama) and additionally parses settings such as AI temperature and timeout.
     * Defaults are applied when respective environment variables are not configured.
     *
     * @return a fully initialized {@link Config} instance containing all API-specific configurations
     *         and general application settings such as temperature and timeout.
     */
    public static Config loadFromEnv() {
        OpenAiConfig openai = new OpenAiConfig(
                System.getenv("OPENAI_API_KEY"),
                getEnvOrDefault("OPENAI_BASE_URL", "https://api.openai.com"),
                getEnvOrDefault("OPENAI_MODEL", "gpt-4o-mini")
        );

        AnthropicConfig anthropic = new AnthropicConfig(
                System.getenv("ANTHROPIC_API_KEY"),
                getEnvOrDefault("ANTHROPIC_MODEL", "claude-sonnet-4-0")
        );

        GoogleConfig google = new GoogleConfig(
                System.getenv("GOOGLE_API_KEY"),
                getEnvOrDefault("GOOGLE_MODEL", "gemini-2.0-flash")
        );

        DeepseekConfig deepseek = new DeepseekConfig(
                System.getenv("DEEPSEEK_API_KEY")
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
                anthropic,
                google,
                deepseek,
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
     * or if the parsed value is not within the range [0.0, 2.0], a default value of {@code 0.1} is returned.
     *
     * @param value the string to be parsed into a double.
     * @return the parsed double value between 0.0 and 2.0 (inclusive), or 0.1 if the input is invalid or out of range.
     */
    private static double parseDouble(String value) {
        try {
            double parsed = Double.parseDouble(value);
            return (parsed < 0.0 || parsed > 2.0) ? 0.1 : parsed;
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
     * Represents the configuration settings for the Anthropic API.
     * This record contains the necessary credentials and model information
     * to interact with the Anthropic services.
     *
     * @param apiKey the API key used to authenticate requests to the Anthropic API.
     * @param model  the name of the model to use for Anthropic operations.
     */
    public record AnthropicConfig(String apiKey, String model){
        public boolean isConfigured() {
            return apiKey != null && !apiKey.isBlank();
        }
    }

    /**
     * Represents the configuration settings required for Deepseek operations.
     * This record holds the necessary API key to authenticate requests.
     *
     * @param apiKey the API key used to authenticate requests for Deepseek.
     */
    public record DeepseekConfig(String apiKey){
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
