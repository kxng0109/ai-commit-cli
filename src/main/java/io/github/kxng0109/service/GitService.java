package io.github.kxng0109.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
     * @param timeoutSeconds    the maximum operation timeout in seconds, from 1 to 3599;
     *                          Git commands exceeding this duration are forcefully terminated
     * @param workingDirectory  the absolute path of the directory where Git commands should be executed;
     *                          this must be an existing accessible directory
     * @throws IllegalArgumentException when the timeout is out of range or the directory is invalid
     */
    public GitService(int timeoutSeconds, String workingDirectory) {
        if (timeoutSeconds < 1 || timeoutSeconds > 3599) {
            throw new IllegalArgumentException("timeoutSeconds must be between 1 and 3599.");
        }
        if (workingDirectory == null || workingDirectory.isBlank()) {
            throw new IllegalArgumentException("workingDirectory must not be blank.");
        }
        this.timeoutSeconds = timeoutSeconds;
        this.workingDirectory = canonicalize(workingDirectory);
    }

    /**
     * Constructs a new {@code GitService} instance with a specified operation timeout.
     * The working directory for Git operations is automatically detected using
     * a platform-appropriate mechanism.
     *
     * @param timeoutSeconds the maximum operation timeout in seconds, from 1 to 3599.
     * @throws IllegalArgumentException when the timeout is out of range or no usable directory exists
     */
    public GitService(int timeoutSeconds) {
        this(timeoutSeconds, detectWorkingDirectory());
    }

    /**
     * Detects the current working directory, preferring the JVM-controlled {@code user.dir}.
     * <p>
     * The {@code PWD} environment variable is only used as a fallback when {@code user.dir}
     * is unavailable, because it can be spoofed. The result is canonicalized.
     * </p>
     *
     * @return the canonical path to the detected working directory as a {@code String}
     * @throws IllegalStateException when no usable working directory can be determined
     */
    private static String detectWorkingDirectory() {
        String userDir = null;
        try {
            userDir = System.getProperty("user.dir");
        } catch (SecurityException e) {
            log.debug("Cannot read user.dir", e);
        }
        String fallback = new File(".").getAbsoluteFile().getParent();
        return resolveWorkingDirectory(userDir, System.getenv("PWD"), fallback);
    }

    /**
     * Resolves the working directory from explicit candidates, preferring the JVM value.
     * <p>
     * The {@code PWD} environment variable is only used as a fallback when {@code user.dir}
     * is unavailable, because it can be spoofed. The result is canonicalized.
     * </p>
     *
     * @param userDir the {@code user.dir} value, may be {@code null}
     * @param pwd the {@code PWD} value, may be {@code null}
     * @param fallback the last-resort path, may be {@code null}
     * @return the canonical path to the resolved working directory
     * @throws IllegalStateException when no usable working directory can be determined
     */
    static String resolveWorkingDirectory(String userDir, String pwd, String fallback) {
        if (userDir != null && !userDir.isBlank()) {
            File userDirFile = new File(userDir);
            if (userDirFile.isDirectory() && userDirFile.canRead()) {
                log.debug("Detected working directory from user.dir");
                return canonicalize(userDir);
            }
        }

        if (pwd != null && !pwd.isBlank()) {
            File pwdFile = new File(pwd);
            if (pwdFile.isDirectory() && pwdFile.canRead()) {
                log.debug("Detected working directory from PWD: {}", pwd);
                return canonicalize(pwd);
            }
        }

        if (fallback != null && !fallback.isBlank()) {
            File fallbackFile = new File(fallback);
            if (fallbackFile.isDirectory() && fallbackFile.canRead()) {
                log.debug("Using fallback working directory: {}", fallback);
                return canonicalize(fallback);
            }
        }
        throw new IllegalStateException("Cannot determine a usable working directory.");
    }

    /**
     * Canonicalizes a directory path, resolving symlinks and relative segments.
     *
     * @param path the path to canonicalize, must not be {@code null}
     * @return the canonical path
     * @throws IllegalArgumentException when the path is not an accessible directory
     */
    private static String canonicalize(String path) {
        File file = new File(path);
        if (!file.isDirectory() || !file.canRead()) {
            throw new IllegalArgumentException("workingDirectory must be an existing readable directory.");
        }
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            throw new IllegalArgumentException("workingDirectory cannot be resolved.", e);
        }
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
     * @param message the commit message describing the changes being committed,
     *                must not be {@code null} or blank
     * @return the output of the Git commit command as a string, typically containing
     * information about the success of the commit operation
     * @throws IllegalArgumentException when the message is {@code null} or blank
     */
    public String commit(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("commit message must not be blank.");
        }
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
     * <p>
     * Fatal git failures (non-repository, missing binary, timeout) propagate as
     * {@code RuntimeException} instead of masquerading as no changes.
     * </p>
     *
     * @return {@code true} if there are staged changes in the repository, {@code false} otherwise.
     * @throws RuntimeException when the underlying git command fails
     */
    public boolean hasStagedChanges() {
        log.debug("Checking if staged changes...");
        String diff = getStagedDiff();
        return diff != null && !diff.isEmpty();
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
        // Never leak AI credentials to repo-controlled git hooks
        scrubSecretEnv(processBuilder.environment());

        Process process = null;
        try {
            process = processBuilder.start();

            StringBuilder outputBuilder = new StringBuilder();
            StringBuilder errorBuilder = new StringBuilder();
            AtomicBoolean outputTruncated = new AtomicBoolean(false);
            AtomicBoolean errorTruncated = new AtomicBoolean(false);

            Process finalProcess1 = process;
            Thread outputThread = new Thread(() -> {
                drainBounded(finalProcess1.getInputStream(), outputBuilder, outputTruncated);
            }, "git-stdout-drain");
            outputThread.setDaemon(true);

            Process finalProcess = process;
            Thread errorThread = new Thread(() -> {
                drainBounded(finalProcess.getErrorStream(), errorBuilder, errorTruncated);
            }, "git-stderr-drain");
            errorThread.setDaemon(true);

            outputThread.start();
            errorThread.start();

            // Just to prevent the process from hanging indefinitely
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                closeQuietly(process.getInputStream());
                closeQuietly(process.getErrorStream());
                closeQuietly(process.getOutputStream());
                outputThread.join(1000);
                errorThread.join(1000);
                throw new RuntimeException(String.format(
                        "Timeout waiting for %s after %d seconds.", describeCommand(command), timeoutSeconds));
            }

            outputThread.join();
            errorThread.join();

            if (outputTruncated.get() || errorTruncated.get()) {
                throw new RuntimeException(
                        String.format("Command ('%s') output exceeded %d bytes and was truncated; refusing to proceed. "
                                + "Reduce staged changes («git diff --staged --stat») and retry.",
                                String.join(" ", command), SecretScanner.MAX_DIFF_BYTES));
            }

            String output = outputBuilder.toString().trim();
            String error = errorBuilder.toString().trim();
            int exitCode = process.exitValue();

            if (exitCode != 0) {
                log.debug("Git command failed: {} exit={} stdout={} stderr={}",
                        describeCommand(command), exitCode, output, error);
                String detail = error.isEmpty() ? output : error;
                throw new RuntimeException(String.format(
                        "%s failed with exit code %d: %s",
                        describeCommand(command),
                        exitCode,
                        truncate(detail, 500)
                ));
            }

            return output;
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format(
                            "Failed to start %s. Ensure git is installed and the directory is accessible.",
                            describeCommand(command)
                    ),
                    e
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(String.format(
                    "%s interrupted.",
                    describeCommand(command)
            ), e);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    /**
     * Closes a stream without throwing, to unblock stream-drain threads on timeout.
     *
     * @param stream the stream to close, may be {@code null}
     */
    private static void closeQuietly(Closeable stream) {
        if (stream == null) {
            return;
        }
        try {
            stream.close();
        } catch (IOException e) {
            log.debug("Error closing process stream", e);
        }
    }

    /**
     * Removes AI credentials from a process environment so repo-controlled git hooks inherit none.
     * <p>
     * Entries ending in {@code _API_KEY} or starting with {@code AI_} are removed.
     * </p>
     *
     * @param environment the mutable process environment, must not be {@code null}
     */
    static void scrubSecretEnv(Map<String, String> environment) {
        environment.keySet().removeIf(key -> key.endsWith("_API_KEY") || key.startsWith("AI_"));
    }

    /**
     * Describes a git command by binary and subcommand only, never logging arguments.
     * <p>
     * Arguments may carry commit messages or paths, so they stay out of user-facing errors.
     * </p>
     *
     * @param command the command and arguments, must not be {@code null}
     * @return a short description such as {@code git commit}
     */
    private static String describeCommand(String... command) {
        if (command.length > 1) {
            return command[0] + " " + command[1];
        }
        return command.length == 1 ? command[0] : "git";
    }

    /**
     * Truncates text for user-facing messages.
     *
     * @param text the text to truncate, may be {@code null}
     * @param maxLength the maximum length to keep
     * @return the truncated text, or an empty string when {@code null}
     */
    private static String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "…";
    }

    /**
     * Drains a process stream with a hard byte cap to prevent memory exhaustion.
     * <p>
     * Reads up to {@code MAX_DIFF_BYTES + 1} bytes; when more data exists the
     * remainder is discarded so the child never blocks on a full pipe, and the
     * truncation flag is set for the caller to abort safely.
     * </p>
     *
     * @param stream the process stream to drain, must not be {@code null}
     * @param target the accumulator for decoded text, must not be {@code null}
     * @param truncated set to {@code true} when input exceeds the cap, must not be {@code null}
     */
    private static void drainBounded(InputStream stream, StringBuilder target, AtomicBoolean truncated) {
        try {
            byte[] chunk = stream.readNBytes(SecretScanner.MAX_DIFF_BYTES + 1);
            if (chunk.length > SecretScanner.MAX_DIFF_BYTES) {
                truncated.set(true);
                target.append(new String(chunk, 0, SecretScanner.MAX_DIFF_BYTES, StandardCharsets.UTF_8));
                stream.transferTo(OutputStream.nullOutputStream());
            } else {
                target.append(new String(chunk, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            log.debug("Error reading process stream", e);
        }
    }
}