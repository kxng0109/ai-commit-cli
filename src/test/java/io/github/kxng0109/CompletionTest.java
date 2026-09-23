package io.github.kxng0109;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Guards for bundled shell completions.
 */
@DisplayName("Completions")
public class CompletionTest {

    @ParameterizedTest(name = "{0} script loads with expected content")
    @ValueSource(strings = {"bash", "zsh", "fish", "powershell"})
    @DisplayName("each shell script loads with expected content")
    void eachShell_loads(String shell) throws IOException {
        String resource = "completions/ai-commit." + extension(shell);

        String script = read(resource);

        assertThat(script).contains("ai-commit");
        assertThat(script).contains("dry-run").contains("provider").contains("amend");
        assertThat(script).contains("config").contains("completion");
    }

    @Test
    @DisplayName("run rejects unknown shells")
    void run_rejectsUnknownShell() {
        assertThat(AiCommitCli.run(new String[]{"completion", "tcsh"})).isOne();
    }

    @Test
    @DisplayName("run requires a shell name")
    void run_requiresShell() {
        assertThat(AiCommitCli.run(new String[]{"completion"})).isOne();
    }

    @Test
    @DisplayName("run prints bash completion")
    void run_printsBash() throws IOException {
        assertThat(AiCommitCli.run(new String[]{"completion", "bash"})).isZero();
        assertThat(read("completions/ai-commit.bash")).isNotBlank();
    }

    private static String extension(String shell) {
        return shell.equals("powershell") ? "ps1" : shell;
    }

    private static String read(String resource) throws IOException {
        try (InputStream input = CompletionTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertThat(input).as("resource %s", resource).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
