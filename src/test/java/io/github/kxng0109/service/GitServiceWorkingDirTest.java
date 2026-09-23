package io.github.kxng0109.service;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Guards for working-directory resolution in {@link GitService}.
 */
@DisplayName("GitService working directory")
public class GitServiceWorkingDirTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("valid user dir wins over pwd")
    void validUserDir_wins() throws Exception {
        String resolved = GitService.resolveWorkingDirectory(tempDir.toString(), tempDir.toString(), null);

        assertThat(Path.of(resolved)).isEqualTo(tempDir.toRealPath());
    }

    @Test
    @DisplayName("pwd is used when user dir is unusable")
    void pwd_usedWhenUserDirUnusable() throws Exception {
        String resolved = GitService.resolveWorkingDirectory(
                tempDir.resolve("missing").toString(), tempDir.toString(), null);

        assertThat(Path.of(resolved)).isEqualTo(tempDir.toRealPath());
    }

    @Test
    @DisplayName("fallback is used when user dir and pwd are blank")
    void fallback_usedWhenOthersBlank() throws Exception {
        String resolved = GitService.resolveWorkingDirectory("   ", null, tempDir.toString());

        assertThat(Path.of(resolved)).isEqualTo(tempDir.toRealPath());
    }

    @Test
    @DisplayName("all-invalid candidates throw")
    void allInvalid_throw() {
        assertThatThrownBy(() -> GitService.resolveWorkingDirectory(
                        tempDir.resolve("missing").toString(),
                        tempDir.resolve("also-missing").toString(),
                        tempDir.resolve("nope").toString()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("usable working directory");
    }

    @Test
    @DisplayName("null fallback throws")
    void nullFallback_throws() {
        assertThatThrownBy(() -> GitService.resolveWorkingDirectory(null, null, null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("file path is rejected as working directory")
    void filePath_rejected() throws Exception {
        Path file = Files.createFile(tempDir.resolve("note.txt"));

        assertThatThrownBy(() -> new GitService(5, file.toString()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("readable directory");
    }
}
