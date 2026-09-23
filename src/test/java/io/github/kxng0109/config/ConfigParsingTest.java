package io.github.kxng0109.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards for parsing and provider state in {@link Config}.
 */
@DisplayName("Config parsing")
public class ConfigParsingTest {

    @Test
    @DisplayName("temperature accepts boundaries and valid values")
    void temperature_acceptsBoundaries() {
        assertThat(Config.parseDouble("0.0")).isZero();
        assertThat(Config.parseDouble("2.0")).isEqualTo(2.0);
        assertThat(Config.parseDouble(" 0.7 ")).isEqualTo(0.7);
    }

    @Test
    @DisplayName("temperature falls back on invalid input")
    void temperature_fallsBackOnInvalid() {
        assertThat(Config.parseDouble("-0.1")).isEqualTo(0.1);
        assertThat(Config.parseDouble("2.1")).isEqualTo(0.1);
        assertThat(Config.parseDouble("NaN")).isEqualTo(0.1);
        assertThat(Config.parseDouble("Infinity")).isEqualTo(0.1);
        assertThat(Config.parseDouble("-Infinity")).isEqualTo(0.1);
        assertThat(Config.parseDouble("hot")).isEqualTo(0.1);
    }

    @Test
    @DisplayName("timeout accepts boundaries and valid values")
    void timeout_acceptsBoundaries() {
        assertThat(Config.parseInt("1")).isOne();
        assertThat(Config.parseInt("3599")).isEqualTo(3599);
        assertThat(Config.parseInt(" 45 ")).isEqualTo(45);
    }

    @Test
    @DisplayName("timeout falls back on invalid input")
    void timeout_fallsBackOnInvalid() {
        assertThat(Config.parseInt("0")).isEqualTo(30);
        assertThat(Config.parseInt("3600")).isEqualTo(30);
        assertThat(Config.parseInt("-5")).isEqualTo(30);
        assertThat(Config.parseInt("forever")).isEqualTo(30);
    }

    @Test
    @DisplayName("timeout prefers canonical name over legacy alias")
    void timeout_prefersCanonical() {
        assertThat(Config.parseTimeout("45", "60")).isEqualTo(45);
        assertThat(Config.parseTimeout(" 45 ", "60")).isEqualTo(45);
        assertThat(Config.parseTimeout(null, "60")).isEqualTo(60);
        assertThat(Config.parseTimeout("   ", "60")).isEqualTo(60);
        assertThat(Config.parseTimeout(null, null)).isEqualTo(30);
        assertThat(Config.parseTimeout(null, "   ")).isEqualTo(30);
        assertThat(Config.parseTimeout("bogus", null)).isEqualTo(30);
    }

    @Test
    @DisplayName("insecure http is accepted behind explicit opt-in")
    void insecureHttp_acceptedWithOptIn() {
        assertThat(Config.validateBaseUrl("OPENAI_BASE_URL", "http://proxy.internal:8080", true))
                .isEqualTo("http://proxy.internal:8080");
    }

    @Test
    @DisplayName("scheme-less value is rejected")
    void schemeLess_rejected() {
        assertThatThrownBy(() -> Config.validateBaseUrl("OPENAI_BASE_URL", "api.example.com", false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("http or https");
    }

    @Test
    @DisplayName("null value is rejected")
    void nullValue_rejected() {
        assertThatThrownBy(() -> Config.validateBaseUrl("OPENAI_BASE_URL", null, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    @DisplayName("blank keys never count as configured")
    void blankKeys_neverConfigured() {
        assertThat(new Config.OpenAiConfig("   ", "https://api.openai.com", "gpt-4o-mini").isConfigured())
                .isFalse();
        assertThat(new Config.AnthropicConfig("", "claude-sonnet-4-0").isConfigured()).isFalse();
        assertThat(new Config.GoogleConfig("   ", "gemini-2.0-flash").isConfigured()).isFalse();
        assertThat(new Config.DeepseekConfig("").isConfigured()).isFalse();
        assertThat(new Config.OllamaConfig("  ", "http://localhost:11434").isConfigured()).isFalse();
    }

    @Test
    @DisplayName("preferences holder instantiates")
    void preferences_instantiates() {
        assertThat(new UserPreferences()).isNotNull();
    }
}
