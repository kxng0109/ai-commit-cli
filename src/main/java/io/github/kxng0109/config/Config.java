package io.github.kxng0109.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 *                             from 0.0 (deterministic) to 2.0 (high creativity).
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
    private static final Logger log = LoggerFactory.getLogger(Config.class);
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
                blankToNull(System.getenv("OPENAI_API_KEY")),
                validateBaseUrl("OPENAI_BASE_URL",
                        getEnvOrDefault("OPENAI_BASE_URL", "https://api.openai.com")),
                getEnvOrDefault("OPENAI_MODEL", "gpt-4o-mini")
        );

        AnthropicConfig anthropic = new AnthropicConfig(
                blankToNull(System.getenv("ANTHROPIC_API_KEY")),
                getEnvOrDefault("ANTHROPIC_MODEL", "claude-sonnet-4-0")
        );

        GoogleConfig google = new GoogleConfig(
                blankToNull(System.getenv("GOOGLE_API_KEY")),
                getEnvOrDefault("GOOGLE_MODEL", "gemini-2.0-flash")
        );

        DeepseekConfig deepseek = new DeepseekConfig(
                blankToNull(System.getenv("DEEPSEEK_API_KEY"))
        );

        OllamaConfig ollama = new OllamaConfig(
                blankToNull(System.getenv("OLLAMA_MODEL")),
                validateBaseUrl("OLLAMA_BASE_URL",
                        getEnvOrDefault("OLLAMA_BASE_URL", "http://localhost:11434"))
        );

        double temperature = parseDouble(
                getEnvOrDefault("AI_TEMPERATURE", "0.1")
        );

        int timeout = parseTimeout();

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
        return (value == null || value.isBlank()) ? defaultValue : value.trim();
    }

    /**
     * Returns a trimmed value or {@code null} when blank, for optional secrets and names.
     *
     * @param value the raw value, may be {@code null}
     * @return the trimmed value, or {@code null} when {@code null} or blank
     */
    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    /**
     * Validates a provider base URL and rejects SSRF-prone values.
     * <p>
     * Only {@code http} and {@code https} schemes are accepted, credentials in the
     * URL are rejected, and plain {@code http} is limited to loopback hosts unless
     * {@code AI_ALLOW_INSECURE_HTTP} is truthy.
     * </p>
     *
     * @param name the environment variable name for error messages, must not be {@code null}
     * @param value the URL value to validate, must not be {@code null}
     * @return the trimmed valid URL
     * @throws IllegalStateException when the URL is blank, malformed, or unsafe
     */
    static String validateBaseUrl(String name, String value) {
        return validateBaseUrl(name, value, isTruthy(System.getenv("AI_ALLOW_INSECURE_HTTP")));
    }

    /**
     * Validates a provider base URL with an explicit insecure-http allowance.
     * <p>
     * Only {@code http} and {@code https} schemes are accepted, credentials in the
     * URL are rejected, and plain {@code http} is limited to loopback hosts unless
     * {@code allowInsecureHttp} is {@code true}.
     * </p>
     *
     * @param name the environment variable name for error messages, must not be {@code null}
     * @param value the URL value to validate, must not be {@code null}
     * @param allowInsecureHttp whether plain http is accepted for non-loopback hosts
     * @return the trimmed valid URL
     * @throws IllegalStateException when the URL is blank, malformed, or unsafe
     */
    static String validateBaseUrl(String name, String value, boolean allowInsecureHttp) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalStateException(name + " must not be blank.");
        }
        URI uri;
        try {
            uri = new URI(trimmed);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(name + " is not a valid URL.", e);
        }
        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new IllegalStateException(name + " must use http or https.");
        }
        if (uri.getUserInfo() != null) {
            throw new IllegalStateException(name + " must not contain credentials.");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalStateException(name + " must include a host.");
        }
        if (scheme.equalsIgnoreCase("http") && !isLoopbackHost(host) && !allowInsecureHttp) {
            throw new IllegalStateException(name + " uses plain http for a non-local host; "
                    + "use https or set AI_ALLOW_INSECURE_HTTP=true.");
        }
        return trimmed;
    }

    /**
     * Checks whether a host is a loopback address.
     *
     * @param host the host to check, must not be {@code null}
     * @return {@code true} for localhost, 127.x, or ::1 forms
     */
    static boolean isLoopbackHost(String host) {
        String lower = host.toLowerCase(Locale.ROOT);
        return lower.equals("localhost") || lower.startsWith("127.")
                || lower.equals("::1") || lower.equals("[::1]");
    }

    /**
     * Parses a truthy switch value such as on, true, 1, or yes.
     *
     * @param value the raw value, may be {@code null}
     * @return {@code true} for on, true, 1, or yes (case-insensitive)
     */
    static boolean isTruthy(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("true") || normalized.equals("1")
                || normalized.equals("yes") || normalized.equals("on");
    }

    /**
     * Resolves the git command timeout, preferring {@code AI_COMMAND_TIMEOUT}.
     * <p>
     * {@code AI_TIMEOUT} remains accepted as a deprecated alias.
     * </p>
     *
     * @return the timeout in seconds between 1 and 3599, or 30 when unset or invalid
     */
    static int parseTimeout() {
        return parseTimeout(System.getenv("AI_COMMAND_TIMEOUT"), System.getenv("AI_TIMEOUT"));
    }

    /**
     * Resolves the git command timeout from explicit values, preferring the canonical name.
     * <p>
     * {@code AI_TIMEOUT} remains accepted as a deprecated alias.
     * </p>
     *
     * @param canonical the {@code AI_COMMAND_TIMEOUT} value, may be {@code null}
     * @param legacy the {@code AI_TIMEOUT} value, may be {@code null}
     * @return the timeout in seconds between 1 and 3599, or 30 when unset or invalid
     */
    static int parseTimeout(String canonical, String legacy) {
        if (canonical != null && !canonical.isBlank()) {
            return parseInt(canonical.trim());
        }
        if (legacy != null && !legacy.isBlank()) {
            log.warn("AI_TIMEOUT is deprecated; use AI_COMMAND_TIMEOUT.");
            return parseInt(legacy.trim());
        }
        return 30;
    }

    /**
     * Parses the given string into a double value. If the parsing fails due to a {@code NumberFormatException},
     * or if the parsed value is not within the range [0.0, 2.0], a default value of {@code 0.1} is returned.
     *
     * @param value the string to be parsed into a double.
     * @return the parsed double value between 0.0 and 2.0 (inclusive), or 0.1 if the input is invalid or out of range.
     */
    static double parseDouble(String value) {
        try {
            double parsed = Double.parseDouble(value.trim());
            if (!Double.isFinite(parsed) || parsed < 0.0 || parsed > 2.0) {
                log.warn("Invalid AI_TEMPERATURE; using default 0.1.");
                return 0.1;
            }
            return parsed;
        } catch (NumberFormatException e) {
            log.warn("Invalid AI_TEMPERATURE; using default 0.1.");
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
    static int parseInt(String value) {
        try {
            int i = Integer.parseInt(value.trim());
            if (i > 0 && i < 3600) {
                return i;
            }
            log.warn("AI timeout out of range; using default 30 seconds.");
            return 30;
        } catch (NumberFormatException e) {
            log.warn("Invalid AI timeout; using default 30 seconds.");
            return 30;
        }
    }

    /**
     * Returns a copy of this config with the model replaced for one provider.
     * <p>
     * Supported providers are {@code openai}, {@code anthropic}, {@code google},
     * {@code ollama}, and {@code deepseek}. The DeepSeek provider uses a fixed
     * model and rejects overrides.
     * </p>
     *
     * @param provider the provider name (case-insensitive), must not be {@code null}
     * @param model the replacement model, must not be blank
     * @return a new config with the model replaced
     * @throws IllegalArgumentException when the provider is unknown, blank, or fixed-model,
     * or when the model is blank
     */
    public Config withModel(String provider, String model) {
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException(
                    "Unknown provider ''. Supported: openai, anthropic, google, deepseek, ollama.");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("Model must not be blank.");
        }
        String name = provider.trim().toLowerCase(Locale.ROOT);
        String trimmedModel = model.trim();
        switch (name) {
            case "openai":
                return new Config(
                        new OpenAiConfig(openai.apiKey(), openai.baseUrl(), trimmedModel),
                        anthropic, google, deepseek, ollama, temperature, commandTimeoutSeconds);
            case "anthropic":
                return new Config(
                        openai,
                        new AnthropicConfig(anthropic.apiKey(), trimmedModel),
                        google, deepseek, ollama, temperature, commandTimeoutSeconds);
            case "google":
                return new Config(
                        openai, anthropic,
                        new GoogleConfig(google.apiKey(), trimmedModel),
                        deepseek, ollama, temperature, commandTimeoutSeconds);
            case "ollama":
                return new Config(
                        openai, anthropic, google, deepseek,
                        new OllamaConfig(trimmedModel, ollama.baseUrl()),
                        temperature, commandTimeoutSeconds);
            case "deepseek":
                throw new IllegalArgumentException(
                        "The deepseek provider uses a fixed model and does not accept --model.");
            default:
                throw new IllegalArgumentException(
                        "Unknown provider '" + provider.trim()
                                + "'. Supported: openai, anthropic, google, deepseek, ollama.");
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
