package io.github.kxng0109.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * The {@code GitService} class provides a simplified and programmatic approach to
 * interacting with a Git repository. It supports common Git operations such as
 * retrieving staged diffs, committing changes, pushing changes, and checking for staged changes.
 *
 * The class enforces a specified timeout for operations to ensure commands
 * do not hang indefinitely. Additionally, it allows setting a working directory
 * for Git command executions.
 *
 * <p>Key Features:
 * <ul>
 *   <li>Detect working directory using the current environment or a fallback mechanism</li>
 *   <li>Retrieve the diff of staged changes</li>
 *   <li>Commit changes with custom messages</li>
 *   <li>Push changes to remote repository</li>
 *   <li>Safeguard against operations exceeding a set timeout duration</li>
 *   <li>Execute custom Git commands with error handling</li>
 * </ul>
 */
public class GitService {

    private static final Logger log = LoggerFactory.getLogger(GitService.class);

    private final int timeoutSeconds;
    private final String workingDirectory;

    /**
     * Constructs a new {@code GitService} instance with a specified operation timeout and working directory.
     * This enables timeout control and specifies the directory where Git operations will be performed.
     *
     * @param timeoutSeconds    the maximum operation timeout in seconds; Git commands exceeding this duration
     *                          will be forcefully terminated to prevent indefinite hangs
     * @param workingDirectory  the absolute path of the directory where Git commands should be executed;
     *                          this must be a valid and accessible directory path
     */
    public GitService(int timeoutSeconds, String workingDirectory) {
        this.timeoutSeconds = timeoutSeconds;
        this.workingDirectory = workingDirectory;
    }

    /**
     * Constructs a new {@code GitService} instance with a specified operation timeout.
     * The working directory for Git operations is automatically detected using
     * a platform-appropriate mechanism.
     *
     * @param timeoutSeconds the maximum operation timeout in seconds. This value determines
     *                        the duration after which Git commands are forcefully terminated
     *                        to prevent hanging.
     */
    public GitService(int timeoutSeconds) {
        this(timeoutSeconds, detectWorkingDirectory());
    }

    /**
     * Detects the current working directory by attempting to retrieve it using the `PWD` environment variable
     * (primarily for Unix-like systems such as MacOS and Linux) or by falling back to the absolute path of the
     * current directory if the `PWD` variable is unavailable or invalid.
     *
     * @return the absolute path to the detected working directory as a {@code String}. Returns the value of the `PWD`
     * environment variable if valid and points to an existing directory; otherwise, falls back to the absolute path
     * of the current directory.
     */
    private static String detectWorkingDirectory() {
        //For MacOS and Linux
        String pwd = System.getenv("PWD");
        if (pwd != null && !pwd.isEmpty()) {
            File pwdFile = new File(pwd);
            if (pwdFile.exists() && pwdFile.isDirectory()) {
                log.debug("Detected working directory from PWD: {}", pwd);
                return pwd;
            }
        }

        // Fallback: get absolute path of current directory
        String fallback = new File(".").getAbsoluteFile().getParent();
        log.debug("Using fallback working directory: {}", fallback);
        return new File(".").getAbsoluteFile().getParent();
    }

    /**
     * Retrieves the diff of staged changes in the current Git repository.
     * This method executes the Git command to fetch differences for files
     * that have been staged for commit, allowing the user to review the changes.
     *
     * @return a {@code String} containing the diff of staged files. If no changes
     *         are staged, the return value will be an empty string.
     */
    public String getStagedDiff() {
        log.debug("Retrieving staged changes...");
        String result = runCommand("git", "--no-pager", "diff", "--staged");
        log.debug("Retrieved {} characters of staged diff", result.length());
        return result;
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
     * Pushes changes to the remote Git repository using the `git push` command.
     * This method executes the push operation in the context of the configured 
     * working directory and logs the operation for debugging purposes.
     *
     * @return the output of the `git push` command as a {@code String}. Typically includes
     *         information about the success or failure of the push operation.
     */
    public String push(){
        log.debug("Pushing changes...");
        return runCommand("git", "push");
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
     * Executes a shell command using the specified arguments and returns the resulting output as a string.
     * The method utilizes a {@link ProcessBuilder} to execute the command in a specified working directory,
     * captures both standard output and error streams, and enforces a timeout to prevent hanging processes.
     * If the command fails or the process exceeds the designated timeout, an appropriate exception is thrown.
     *
     * @param command the command to execute, represented as a variable-length argument array of strings.
     *                Each string corresponds to a part of the command or its arguments.
     * @return the standard output of the executed command as a {@code String}, trimmed of leading and trailing whitespace.
     * @throws RuntimeException if the command execution fails, times out, or if the process exits with a non-zero status.
     */
    private String runCommand(String... command) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);

        File workingDir = new File(workingDirectory);
        log.debug("Using working directory: {}", workingDir.getAbsolutePath());

        processBuilder.directory(workingDir);

        // Disable Git pager to prevent interactive prompts
        processBuilder.environment().put("GIT_PAGER", "cat");

        Process process = null;
        try {
            process = processBuilder.start();

            StringBuilder outputBuilder = new StringBuilder();
            StringBuilder errorBuilder = new StringBuilder();

            Process finalProcess1 = process;
            Thread outputThread = new Thread(() -> {
                try {
                    outputBuilder.append(new String(finalProcess1.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
                } catch (IOException e) {
                    log.debug("Error reading output stream", e);
                }
            });

            Process finalProcess = process;
            Thread errorThread = new Thread(() -> {
                try {
                    errorBuilder.append(new String(finalProcess.getErrorStream().readAllBytes(), StandardCharsets.UTF_8));
                } catch (IOException e) {
                    log.debug("Error reading error stream", e);
                }
            });

            outputThread.start();
            errorThread.start();

            // Just to prevent the process from hanging indefinitely
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            outputThread.join(1000);
            errorThread.join(1000);

            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException(
                        String.format("Timeout while waiting for process('%s') to finish", String.join(" ", command)));
            }

            String output = outputBuilder.toString().trim();
            String error = errorBuilder.toString().trim();
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