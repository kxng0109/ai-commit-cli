package io.github.kxng0109.service;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Guards for fail-fast validation in {@link GitService}.
 */
@DisplayName("GitService validation")
public class GitServiceValidationTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("timeout below range is rejected")
    void timeoutBelowRange_isRejected() {
        assertThatThrownBy(() -> new GitService(0, tempDir.toString()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1 and 3599");
    }

    @Test
    @DisplayName("timeout above range is rejected")
    void timeoutAboveRange_isRejected() {
        assertThatThrownBy(() -> new GitService(3600, tempDir.toString()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1 and 3599");
    }

    @Test
    @DisplayName("blank working directory is rejected")
    void blankDirectory_isRejected() {
        assertThatThrownBy(() -> new GitService(5, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    @DisplayName("missing working directory is rejected")
    void missingDirectory_isRejected() {
        Path missing = tempDir.resolve("does-not-exist");

        assertThatThrownBy(() -> new GitService(5, missing.toString()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("readable directory");
    }

    @Test
    @DisplayName("null commit message is rejected before git")
    void nullCommitMessage_isRejected() {
        GitService service = new GitService(5, tempDir.toString());

        assertThatThrownBy(() -> service.commit(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    @DisplayName("blank commit message is rejected before git")
    void blankCommitMessage_isRejected() {
        GitService service = new GitService(5, tempDir.toString());

        assertThatThrownBy(() -> service.commit("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    @DisplayName("boundary timeouts are accepted")
    void boundaryTimeouts_areAccepted() {
        assertThat(new GitService(1, tempDir.toString())).isNotNull();
        assertThat(new GitService(3599, tempDir.toString())).isNotNull();
    }
}
