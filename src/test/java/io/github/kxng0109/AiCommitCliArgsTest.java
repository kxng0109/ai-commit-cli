package io.github.kxng0109;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards for command-line parsing in {@link AiCommitCli}.
 */
@DisplayName("AiCommitCli args")
public class AiCommitCliArgsTest {

    @Test
    @DisplayName("null and empty args yield empty options")
    void nullAndEmpty_yieldEmpty() {
        AiCommitCli.CliOptions nullOptions = AiCommitCli.parseArgs(null);
        AiCommitCli.CliOptions emptyOptions = AiCommitCli.parseArgs(new String[0]);

        assertThat(nullOptions.subcommand()).isNull();
        assertThat(emptyOptions.subcommand()).isNull();
        assertThat(emptyOptions.subArgs()).isEmpty();
        assertThat(emptyOptions.model()).isNull();
        assertThat(emptyOptions.provider()).isNull();
    }

    @Test
    @DisplayName("boolean flags parse in long and short forms")
    void booleanFlags_parse() {
        AiCommitCli.CliOptions options = AiCommitCli.parseArgs(
                new String[]{"--yes", "--dry-run", "--amend", "-a", "-v", "-h"});

        assertThat(options.yes()).isTrue();
        assertThat(options.dryRun()).isTrue();
        assertThat(options.amend()).isTrue();
        assertThat(options.stageAll()).isTrue();
        assertThat(options.version()).isTrue();
        assertThat(options.help()).isTrue();
    }

    @Test
    @DisplayName("short aliases parse")
    void shortAliases_parse() {
        AiCommitCli.CliOptions options = AiCommitCli.parseArgs(new String[]{"-y", "--all"});

        assertThat(options.yes()).isTrue();
        assertThat(options.stageAll()).isTrue();
    }

    @Test
    @DisplayName("model and provider accept space and equals forms")
    void modelAndProvider_forms() {
        AiCommitCli.CliOptions spaced = AiCommitCli.parseArgs(
                new String[]{"--model", "gpt-4o", "--provider", "openai"});

        assertThat(spaced.model()).isEqualTo("gpt-4o");
        assertThat(spaced.provider()).isEqualTo("openai");

        AiCommitCli.CliOptions inlined = AiCommitCli.parseArgs(
                new String[]{"--model=gpt-4o", "--provider=openai"});

        assertThat(inlined.model()).isEqualTo("gpt-4o");
        assertThat(inlined.provider()).isEqualTo("openai");
    }

    @Test
    @DisplayName("last value wins for repeated options")
    void repeatedOptions_lastWins() {
        AiCommitCli.CliOptions options = AiCommitCli.parseArgs(
                new String[]{"--model", "a", "--model", "b"});

        assertThat(options.model()).isEqualTo("b");
    }

    @Test
    @DisplayName("missing values are rejected")
    void missingValues_rejected() {
        assertThatThrownBy(() -> AiCommitCli.parseArgs(new String[]{"--model"}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("--model");
        assertThatThrownBy(() -> AiCommitCli.parseArgs(new String[]{"--provider", "--yes"}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("--provider");
        assertThatThrownBy(() -> AiCommitCli.parseArgs(new String[]{"--model="}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AiCommitCli.parseArgs(new String[]{"--model", "   "}))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("unknown options are rejected")
    void unknownOptions_rejected() {
        assertThatThrownBy(() -> AiCommitCli.parseArgs(new String[]{"--modle", "x"}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("--modle");
        assertThatThrownBy(() -> AiCommitCli.parseArgs(new String[]{"-z"}))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("subcommands collect trailing args")
    void subcommands_collectTrailing() {
        AiCommitCli.CliOptions options = AiCommitCli.parseArgs(
                new String[]{"--yes", "config", "--auto-commit", "on"});

        assertThat(options.yes()).isTrue();
        assertThat(options.subcommand()).isEqualTo("config");
        assertThat(options.subArgs()).containsExactly("--auto-commit", "on");
    }

    @Test
    @DisplayName("completion subcommand parses")
    void completion_parses() {
        AiCommitCli.CliOptions options = AiCommitCli.parseArgs(new String[]{"completion", "bash"});

        assertThat(options.subcommand()).isEqualTo("completion");
        assertThat(options.subArgs()).containsExactly("bash");
    }

    @Test
    @DisplayName("unknown subcommands are rejected")
    void unknownSubcommands_rejected() {
        assertThatThrownBy(() -> AiCommitCli.parseArgs(new String[]{"confgi"}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("confgi");
    }

    @Test
    @DisplayName("sub args list is unmodifiable")
    void subArgs_unmodifiable() {
        AiCommitCli.CliOptions options = AiCommitCli.parseArgs(new String[]{"config", "x"});

        assertThat(options.subArgs()).isEqualTo(List.of("x"));
        assertThatThrownBy(() -> options.subArgs().add("y"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
