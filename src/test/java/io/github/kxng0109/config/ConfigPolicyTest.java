package io.github.kxng0109.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards for provider URL policy and switch parsing in {@link Config}.
 */
@DisplayName("Config policy")
public class ConfigPolicyTest {

    @Test
    @DisplayName("https provider URL is accepted")
    void httpsUrl_isAccepted() {
        assertThat(Config.validateBaseUrl("OPENAI_BASE_URL", "https://api.openai.com"))
                .isEqualTo("https://api.openai.com");
    }

    @Test
    @DisplayName("http loopback URL is accepted")
    void httpLoopback_isAccepted() {
        assertThat(Config.validateBaseUrl("OLLAMA_BASE_URL", "http://localhost:11434"))
                .isEqualTo("http://localhost:11434");
        assertThat(Config.validateBaseUrl("OLLAMA_BASE_URL", "http://127.0.0.1:11434"))
                .isEqualTo("http://127.0.0.1:11434");
    }

    @Test
    @DisplayName("http remote URL is rejected")
    void httpRemote_isRejected() {
        assertThatThrownBy(() -> Config.validateBaseUrl("OPENAI_BASE_URL", "http://evil.example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("plain http");
    }

    @Test
    @DisplayName("URL with credentials is rejected")
    void urlWithCredentials_isRejected() {
        assertThatThrownBy(
                () -> Config.validateBaseUrl("OPENAI_BASE_URL", "https://user:pass@api.example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("credentials");
    }

    @Test
    @DisplayName("non-http scheme is rejected")
    void nonHttpScheme_isRejected() {
        assertThatThrownBy(() -> Config.validateBaseUrl("OPENAI_BASE_URL", "ftp://api.example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("http or https");
    }

    @Test
    @DisplayName("blank URL is rejected")
    void blankUrl_isRejected() {
        assertThatThrownBy(() -> Config.validateBaseUrl("OPENAI_BASE_URL", "   "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    @DisplayName("malformed URL is rejected")
    void malformedUrl_isRejected() {
        assertThatThrownBy(() -> Config.validateBaseUrl("OPENAI_BASE_URL", "https://exa mple.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not a valid URL");
    }

    @Test
    @DisplayName("loopback detection covers common forms")
    void loopback_detectionCoversCommonForms() {
        assertThat(Config.isLoopbackHost("localhost")).isTrue();
        assertThat(Config.isLoopbackHost("LOCALHOST")).isTrue();
        assertThat(Config.isLoopbackHost("127.0.0.1")).isTrue();
        assertThat(Config.isLoopbackHost("127.0.0.2")).isTrue();
        assertThat(Config.isLoopbackHost("::1")).isTrue();
        assertThat(Config.isLoopbackHost("[::1]")).isTrue();
        assertThat(Config.isLoopbackHost("api.example.com")).isFalse();
        assertThat(Config.isLoopbackHost("localhost.evil.com")).isFalse();
    }

    @Test
    @DisplayName("truthy parsing covers switch forms")
    void truthy_parsesSwitchForms() {
        assertThat(Config.isTruthy("true")).isTrue();
        assertThat(Config.isTruthy("1")).isTrue();
        assertThat(Config.isTruthy("yes")).isTrue();
        assertThat(Config.isTruthy("on")).isTrue();
        assertThat(Config.isTruthy(" ON ")).isTrue();
        assertThat(Config.isTruthy("false")).isFalse();
        assertThat(Config.isTruthy("0")).isFalse();
        assertThat(Config.isTruthy(null)).isFalse();
        assertThat(Config.isTruthy("maybe")).isFalse();
    }

    @Test
    @DisplayName("timeout resolves within range")
    void timeout_resolvesWithinRange() {
        assertThat(Config.parseTimeout()).isBetween(1, 3599);
    }
}
