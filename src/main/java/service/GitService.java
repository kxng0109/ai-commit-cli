package service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Provides Git-related operations such as retrieving staged changes, committing them,
 * and checking for their presence. This class interacts with the Git command-line tool
 * to execute and manage these operations programmatically.
 */
public class GitService {

    private static final Logger log = LoggerFactory.getLogger(GitService.class);

    private final int timeoutSeconds;

    public GitService(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * Retrieves the differences of the currently staged changes in the Git repository.
     * This method executes the Git command to fetch the staged diff.
     *
     * @return a string containing the output of the Git diff command for the staged changes,
     *         which represents the differences of the files that are staged for commit.
     */
    public String getStagedDiff() {
        log.debug("Retrieving staged changes...");
        return runCommand("git", "diff", "--staged");
    }

    /**
     * Commits the currently staged changes in the Git repository with the provided commit message.
     * This method executes a Git commit command and logs the operation.
     *
     * @param message the commit message to associate with the staged changes
     * @return the output of the Git commit command as a trimmed string
     */
    public String commit(String message){
        log.debug("Committing changes...");
        return runCommand("git", "commit", "--message", message);
    }

    /**
     * Checks if there are staged changes in the Git repository.
     * This method determines the presence of changes that are staged
     * for commit by examining the output of the Git diff command.
     *
     * @return true if there are staged changes present, false otherwise
     */
    public boolean hasStagedChanges(){
        log.debug("Checking if staged changes...");
        try{
            String diff = getStagedDiff();
            return diff != null && !diff.isEmpty();
        }catch (RuntimeException e){
            log.debug("Failed to check for staged changes", e);
            return false;
        }
    }

    /**
     * Executes the given command as a system process.
     * The method will wait for the process to complete within the specified timeout duration.
     * If the process takes longer than the timeout or exits unsuccessfully, it will throw an exception.
     *
     * @param command the command to execute, where each part of the command is passed as a separate argument
     *                (e.g., "git", "diff", "--staged").
     * @return the standard output of the command as a trimmed string.
     * @throws RuntimeException if the process fails, times out, or is interrupted.
     */
    private String runCommand(String... command) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
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
