package io.github.kxng0109.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * A service class that provides utility methods for interacting with a Git repository.
 * This class supports operations such as retrieving staged changes, committing them,
 * and checking for staged changes. It utilizes underlying Git commands for execution.
 */
public class GitService {

    private static final Logger log = LoggerFactory.getLogger(GitService.class);

    private final int timeoutSeconds;

    public GitService(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * Retrieves the differences between the currently staged changes and the repository's HEAD.
     * This method uses the Git command `git diff --staged` to fetch the diff of files
     * that have been staged for commit.
     *
     * @return a string representing the staged differences in unified diff format.
     */
    public String getStagedDiff() {
        log.debug("Retrieving staged changes...");
        return runCommand("git", "diff", "--staged");
    }

    /**
     * Commits the currently staged changes in the Git repository with the provided commit message.
     * This method invokes the Git command to commit changes and adds the specified message.
     *
     * @param message the commit message describing the changes being committed
     * @return the output of the Git commit command as a string, typically containing
     * information about the success of the commit operation
     */
    public String commit(String message) {
        log.debug("Committing changes...");
        return runCommand("git", "commit", "--message", message);
    }

    /**
     * Checks whether there are staged changes in the Git repository.
     * This method determines if any files have been staged for commit by analyzing
     * the output of a Git command retrieving staged differences.
     *
     * @return {@code true} if there are staged changes in the repository, {@code false} otherwise.
     */
    public boolean hasStagedChanges() {
        log.debug("Checking if staged changes...");
        try {
            String diff = getStagedDiff();
            return diff != null && !diff.isEmpty();
        } catch (RuntimeException e) {
            log.debug("Failed to check for staged changes", e);
            return false;
        }
    }

    /**
     * Executes a shell command represented as a sequence of strings and retrieves its output.
     * This method utilizes {@link ProcessBuilder} to run the command, sets the current working
     * directory to the user's directory, and enforces a timeout to avoid indefinite hangs.
     * Errors and non-zero exit codes are handled by throwing runtime exceptions.
     *
     * @param command the command to execute, provided as an array of strings where the first string represents
     *                the command name and subsequent strings represent the arguments
     * @return the standard output of the executed command as a trimmed string
     * @throws RuntimeException if the command fails, times out, or is interrupted
     */
    private String runCommand(String... command) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);

        File workingDir = new File(System.getProperty("user.dir"));
        processBuilder.directory(workingDir);
        processBuilder.redirectErrorStream(true);

        Process process = null;
        try {
            process = processBuilder.start();
            // Just to prevent the process from hanging indefinitely
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException(
                        String.format("Timeout while waiting for process('%s') to finish", String.join(" ", command)));
            }

            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            String error = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8).trim();

            int exitCode = process.exitValue();

            if (exitCode != 0) {
                throw new RuntimeException(String.format(
                        "Command ('%s') failed with exit code: %d\n %s",
                        String.join(" ", command),
                        exitCode,
                        error
                ));
            }

            return output;
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format(
                            "Failed to execute command: %s. %s \n %s ",
                            String.join(" ", command),
                            e.getMessage(),
                            e
                    )
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(String.format(
                    "Command ('%s') interrupted. %s",
                    String.join(" ", command),
                    e
            ));
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }
}