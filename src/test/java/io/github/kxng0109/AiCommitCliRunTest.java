package io.github.kxng0109;

import io.github.kxng0109.config.UserPreferences;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards for CLI entry paths in {@link AiCommitCli}.
 */
@DisplayName("AiCommitCli run")
public class AiCommitCliRunTest {

    @AfterEach
    void cleanup() {
        UserPreferences.reset();
    }

    @Test
    @DisplayName("version flag returns success")
    void versionFlag_returnsSuccess() {
        assertThat(AiCommitCli.run(new String[]{"--version"})).isZero();
        assertThat(AiCommitCli.run(new String[]{"-v"})).isZero();
    }

    @Test
    @DisplayName("help flag returns success")
    void helpFlag_returnsSuccess() {
        assertThat(AiCommitCli.run(new String[]{"--help"})).isZero();
        assertThat(AiCommitCli.run(new String[]{"-h"})).isZero();
    }

    @Test
    @DisplayName("null args are treated as empty")
    void nullArgs_treatedAsEmpty() {
        assumeTrue(noProviderConfigured());

        assertThat(AiCommitCli.run(null)).isOne();
    }

    @Test
    @DisplayName("config without subcommand prints usage")
    void configWithoutSubcommand_printsUsage() {
        assertThat(AiCommitCli.run(new String[]{"config"})).isZero();
    }

    @Test
    @DisplayName("config show returns success")
    void configShow_returnsSuccess() {
        assertThat(AiCommitCli.run(new String[]{"config", "--show"})).isZero();
    }

    @Test
    @DisplayName("config help returns success")
    void configHelp_returnsSuccess() {
        assertThat(AiCommitCli.run(new String[]{"config", "--help"})).isZero();
    }

    @Test
    @DisplayName("config auto-commit on then off round-trips")
    void configAutoCommit_roundTrips() {
        assertThat(AiCommitCli.run(new String[]{"config", "--auto-commit", "on"})).isZero();
        assertThat(UserPreferences.isAutoCommitEnabled()).isTrue();
        assertThat(AiCommitCli.run(new String[]{"config", "--auto-commit", "off"})).isZero();
        assertThat(UserPreferences.isAutoCommitEnabled()).isFalse();
    }

    @Test
    @DisplayName("config auto-commit without value shows current")
    void configAutoCommitWithoutValue_showsCurrent() {
        assertThat(AiCommitCli.run(new String[]{"config", "--auto-commit"})).isZero();
    }

    @Test
    @DisplayName("config auto-commit with invalid value keeps state")
    void configAutoCommitInvalid_keepsState() {
        assertThat(AiCommitCli.run(new String[]{"config", "--auto-commit", "banana"})).isZero();
        assertThat(UserPreferences.isAutoCommitEnabled()).isFalse();
    }

    @Test
    @DisplayName("config auto-push on then off round-trips")
    void configAutoPush_roundTrips() {
        assertThat(AiCommitCli.run(new String[]{"config", "--auto-push", "on"})).isZero();
        assertThat(UserPreferences.isAutoPushEnabled()).isTrue();
        assertThat(AiCommitCli.run(new String[]{"config", "--auto-push", "off"})).isZero();
        assertThat(UserPreferences.isAutoPushEnabled()).isFalse();
    }

    @Test
    @DisplayName("config auto-push without value shows current")
    void configAutoPushWithoutValue_showsCurrent() {
        assertThat(AiCommitCli.run(new String[]{"config", "--auto-push"})).isZero();
    }

    @Test
    @DisplayName("config auto-push with invalid value keeps state")
    void configAutoPushInvalid_keepsState() {
        assertThat(AiCommitCli.run(new String[]{"config", "--auto-push", "maybe"})).isZero();
        assertThat(UserPreferences.isAutoPushEnabled()).isFalse();
    }

    @Test
    @DisplayName("config auto-push notes inactive auto-commit")
    void configAutoPush_notesInactiveAutoCommit() {
        UserPreferences.setAutoCommit(false);

        assertThat(AiCommitCli.run(new String[]{"config", "--auto-push", "on"})).isZero();
    }

    @Test
    @DisplayName("config reset returns success")
    void configReset_returnsSuccess() {
        UserPreferences.setAutoCommit(true);

        assertThat(AiCommitCli.run(new String[]{"config", "--reset"})).isZero();
        assertThat(UserPreferences.isAutoCommitEnabled()).isFalse();
    }

    @Test
    @DisplayName("unknown config subcommand returns success")
    void unknownSubcommand_returnsSuccess() {
        assertThat(AiCommitCli.run(new String[]{"config", "--bogus"})).isZero();
    }

    @Test
    @DisplayName("commit flow without provider returns failure")
    void commitFlowWithoutProvider_returnsFailure() {
        assumeTrue(noProviderConfigured());

        assertThat(AiCommitCli.run(new String[0])).isOne();
    }

    @Test
    @DisplayName("log level resolution allowlists known levels")
    void logLevel_allowlisted() {
        assertThat(AiCommitCli.resolveLogLevel("debug")).isEqualTo("DEBUG");
        assertThat(AiCommitCli.resolveLogLevel("ERROR")).isEqualTo("ERROR");
        assertThat(AiCommitCli.resolveLogLevel("info")).isEqualTo("INFO");
        assertThat(AiCommitCli.resolveLogLevel(null)).isEqualTo("WARN");
        assertThat(AiCommitCli.resolveLogLevel("verbose")).isEqualTo("WARN");
        assertThat(AiCommitCli.resolveLogLevel("   ")).isEqualTo("WARN");
    }

    @Test
    @DisplayName("default constructor exists")
    void defaultConstructor_exists() {
        assertThat(new AiCommitCli()).isNotNull();
    }

    private static boolean noProviderConfigured() {
        return System.getenv("OPENAI_API_KEY") == null
                && System.getenv("ANTHROPIC_API_KEY") == null
                && System.getenv("GOOGLE_API_KEY") == null
                && System.getenv("DEEPSEEK_API_KEY") == null
                && System.getenv("OLLAMA_MODEL") == null;
    }
}
