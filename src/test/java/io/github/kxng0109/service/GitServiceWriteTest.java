package io.github.kxng0109.service;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Guards for staging and amending in {@link GitService} using live repositories.
 */
@DisplayName("GitService stage and amend")
public class GitServiceWriteTest {

    @TempDir
    Path repoDir;

    @Test
    @DisplayName("stageTracked stages modified tracked files only")
    void stageTracked_stagesModifiedTracked() throws Exception {
        initGitRepo(repoDir);
        Path tracked = repoDir.resolve("tracked.txt");
        Files.writeString(tracked, "v1");
        runCommand(repoDir, "git", "add", "tracked.txt");
        runCommand(repoDir, "git", "commit", "-m", "init");
        Files.writeString(tracked, "v2");
        Path untracked = repoDir.resolve("new.txt");
        Files.writeString(untracked, "new");

        GitService service = new GitService(10, repoDir.toString());
        service.stageTracked();

        assertThat(service.hasStagedChanges()).isTrue();
        String status = runCommand(repoDir, "git", "status", "--porcelain");
        assertThat(status).contains("M  tracked.txt");
        assertThat(status).contains("?? new.txt");
    }

    @Test
    @DisplayName("amend replaces the previous message")
    void amend_replacesMessage() throws Exception {
        initGitRepo(repoDir);
        Files.writeString(repoDir.resolve("a.txt"), "a");
        GitService service = new GitService(10, repoDir.toString());
        runCommand(repoDir, "git", "add", "a.txt");
        service.commit("feat: first");

        Files.writeString(repoDir.resolve("b.txt"), "b");
        runCommand(repoDir, "git", "add", "b.txt");
        String output = service.amend("feat: amended");

        assertThat(output).contains("feat: amended");
        String message = runCommand(repoDir, "git", "log", "-1", "--format=%s");
        assertThat(message.trim()).isEqualTo("feat: amended");
    }

    @Test
    @DisplayName("null and blank amend messages are rejected")
    void amend_rejectsBlank() {
        GitService service = new GitService(10, repoDir.toString());

        assertThatThrownBy(() -> service.amend(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.amend("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private void initGitRepo(Path dir) throws Exception {
        runCommand(dir, "git", "init");
        runCommand(dir, "git", "config", "user.email", "test@example.com");
        runCommand(dir, "git", "config", "user.name", "Test User");
    }

    private String runCommand(Path dir, String... command) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(command)
                .directory(dir.toFile())
                .redirectErrorStream(true);
        Process process = builder.start();
        int exitCode = process.waitFor();
        String output = new String(process.getInputStream().readAllBytes());
        if (exitCode != 0) {
            throw new RuntimeException("Command failed: " + String.join(" ", command) + "\n" + output);
        }
        return output;
    }
}
