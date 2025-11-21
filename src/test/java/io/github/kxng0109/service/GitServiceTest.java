package io.github.kxng0109.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for GitService. They require an actual git installation.
 * These are integration tests that verify git command execution.
 */
public class GitServiceTest {

    @TempDir
    Path tempDir;

    private GitService gitService;

    @BeforeEach
    void setUp() {
        gitService = new GitService(5);
    }

    @Test
    void constructor_shouldCreateServiceWithTimeout() {
        GitService service = new GitService(60);

        assertNotNull(service);
    }

    @Test
    void hasStagedChanges_inNonGitDirectory_shouldReturnFalse() throws IOException {
        Path nonGitDir = Files.createTempDirectory("non-git-test");

        try {
            gitService = new GitService(5, nonGitDir.toString());

            boolean result = gitService.hasStagedChanges();

            assertFalse(result, "Should return false for non-git directory");
        } finally {
            deleteDirectoryWithRetry(nonGitDir);
        }
    }

    @Test
    void hasStagedChanges_inGitRepoWithNoChanges_shouldReturnFalse() throws Exception {
        initGitRepo(tempDir);

        gitService = new GitService(5, tempDir.toString());

        boolean result = gitService.hasStagedChanges();

        assertFalse(result, "Should return false when no changes are staged");
    }

    @Test
    void hasStagedChanges_withStagedFiles_shouldReturnTrue() throws Exception {
        initGitRepo(tempDir);
        createAndStageFile(tempDir, "test.txt", "test content");

        gitService = new GitService(5, tempDir.toString());

        boolean result = gitService.hasStagedChanges();

        assertTrue(result, "Should return true when files are staged");
    }

    @Test
    void getStagedDiff_withStagedChanges_shouldReturnDiff() throws Exception {
        initGitRepo(tempDir);
        createAndStageFile(tempDir, "test.txt", "hello world");

        gitService = new GitService(5, tempDir.toString());

        String diff = gitService.getStagedDiff();

        assertNotNull(diff);
        assertFalse(diff.isEmpty());
        assertTrue(diff.contains("test.txt") || diff.contains("hello world"),
                   "Diff should contain file name or content"
        );
    }

    @Test
    void commit_withValidMessage_shouldCommitSuccessfully() throws Exception {
        initGitRepo(tempDir);
        createAndStageFile(tempDir, "test.txt", "content");

        gitService = new GitService(5, tempDir.toString());

        String result = gitService.commit("test: add test file");

        assertNotNull(result);
        assertTrue(result.contains("test: add test file") || result.contains("test.txt"),
                   "Commit output should reference the commit"
        );
    }


    private void deleteDirectoryWithRetry(Path dir) {
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                if (Files.exists(dir)) {
                    Files.walk(dir)
                         .sorted(Comparator.reverseOrder())
                         .forEach(path -> {
                             try {
                                 Files.deleteIfExists(path);
                             } catch (IOException e) {
                                 // Ignore individual file deletion errors
                             }
                         });
                }
                return;
            } catch (IOException e) {
                if (i < maxRetries - 1) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    private void initGitRepo(Path dir) throws Exception {
        runCommand(dir, "git", "init");
        runCommand(dir, "git", "config", "user.email", "test@example.com");
        runCommand(dir, "git", "config", "user.name", "Test User");
    }

    private void createAndStageFile(Path dir, String filename, String content) throws Exception {
        Path file = dir.resolve(filename);
        Files.writeString(file, content);
        runCommand(dir, "git", "add", filename);
    }

    private void runCommand(Path dir, String... command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command)
                .directory(dir.toFile())
                .redirectErrorStream(true);

        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            String output = new String(process.getInputStream().readAllBytes());
            throw new RuntimeException("Command failed: " + String.join(" ", command) + "\n" + output);
        }

        Thread.sleep(50);
    }
}