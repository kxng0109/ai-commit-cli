package io.github.kxng0109.service;

import io.github.kxng0109.config.UserPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.util.List;

/**
 * The {@code CommitService} class provides functionality to streamline Git commit operations
 * by leveraging AI-enhanced commit message generation and interactive user prompts.
 * <p>
 * This service integrates with a Git management system and an AI model to improve the quality
 * of commit messages by suggesting meaningful and concise messages based on staged changes.
 * It supports both interactive and automated workflows, allowing users to review, regenerate,
 * edit, or confirm commit messages before recording changes in the repository, or to automatically
 * commit and push changes based on user preferences.
 * </p>
 *
 * <p>
 * Key features of this service include:
 * <ul>
 *   <li>Integration with {@link GitService} for repository management.</li>
 *   <li>AI-powered commit message generation using {@link ChatModel}.</li>
 *   <li>Interactive console-based prompts for reviewing and editing commit messages.</li>
 * </ul>
 * </p>
 * <p>
 * Fields:
 * <ul>
 *   <li>{@code SYSTEM_PROMPT}: A string constant used internally for system-specific operations.</li>
 *   <li>{@code log}: A logging mechanism to track errors and actions during execution.</li>
 *   <li>{@code gitService}: Facilitates Git-related operations like staging and commits.</li>
 *   <li>{@code chatModel}: Handles AI-driven commit message generation.</li>
 *   <li>{@code reader}: Manages user input for interactive operations through the console.</li>
 * </ul>
 * <p>
 * Primary Methods:
 * <ul>
 *   <li>{@link #generateAndCommit()}: Automatically generates commit messages based on staged changes and
 *       commits them with user approval or automatically based on preferences.</li>
 *   <li>{@link #handleAutoCommit(String, boolean)}: Handles automated commit workflow without user interaction.</li>
 *   <li>{@link #handleInteractiveCommit(String, boolean)}: Manages interactive commit workflow with user prompts.</li>
 *   <li>{@link #handleAutoPush(boolean)}: Handles automatic pushing of changes to remote repository if enabled.</li>
 *   <li>{@link #promptUser()}: Provides an interactive prompt for user action during commit operations
 *       (e.g., accept, regenerate, edit).</li>
 *   <li>{@link #editCommitMessage(String)}: Allows customization of AI-generated commit messages through user input.</li>
 *   <li>{@link #generateMessage(String)}: Creates AI-suggested commit messages from a given Git diff.</li>
 *   <li>{@link #displayMessage(String)}: Formats and displays AI-generated messages for better readability.</li>
 *   <li>{@link #displayGitOutput(String)}: Formats and outputs results of Git commands for user clarity.</li>
 * </ul>
 *
 * <h3>Usage:</h3>
 * A {@code CommitService} instance must be instantiated with dependencies including {@link GitService},
 * {@link ChatModel}, and a {@link BufferedReader}. Once created, the {@code generateAndCommit()} method can
 * be invoked to initiate the commit process, which will follow either an automated or interactive workflow
 * based on user preferences.
 */
public class CommitService {

    private static final String SYSTEM_PROMPT = """
            You are an expert software engineer and a strict maintainer of a large-scale project. You are reviewing a contribution and your sole task is to write the final commit message for it.
            Analyze the provided git diff and generate a single, professional commit message that adheres perfectly to the Conventional Commits specification.
            
            Rules (Must be followed step-by-step):
            
            1. Format: The commit message MUST be structured as:
            <type>(<scope>): <description>
            
            [optional body]
            
            [optional footer]
            
            2. Type (The Prefix):
            You MUST determine the correct type by analyzing the diff's intent. The allowed types are:
            - feat: (A new feature)
            - fix: (A bug fix)
            - refactor: (A code change that neither fixes a bug nor adds a feature)
            - docs: (Documentation-only changes)
            - test: (Adding missing tests or correcting existing tests)
            - style: (Changes that do not affect the meaning of the code: white-space, formatting, missing semi-colons, etc.)
            - chore: (Changes to the build process, auxiliary tools, or dependencies)
            
            3. Scope:
            You MUST infer an optional (scope) from the diff. The scope should be a noun describing the section of the codebase that was changed (e.g., (api), (auth), (database), (ui)). If no single, clear scope exists, you MUST omit it.
            
            4. Description (The Subject Line):
            - The description MUST be brief (max 50 characters).
            - It MUST be written in the imperative mood (e.g., "add feature", not "added feature" or "adds feature").
            - It MUST not be capitalized.
            
            5. Body:
            - A blank line MUST separate the subject from the body.
            - The body is optional. You should only include a body if the changes are complex and require further explanation of the "what" and "why."
            - You MUST wrap all body lines at 72 characters.
            
            6. Breaking Changes:
            If the diff introduces a breaking change, you MUST append a ! to the type(scope) (e.g., refactor(api)!: ...). You MUST also add a BREAKING CHANGE: footer at the end of the message, followed by a description of the breaking change.
            
            7. Output Constraint (CRITICAL):
            Your response MUST contain only the raw, formatted commit message and nothing else.
            - DO NOT include any preamble like "Here is the commit message:".
            - DO NOT include any markdown formatting like ```.
            - The very first character of your response must be the first letter of the type.
            """;

    private static final Logger log = LoggerFactory.getLogger(CommitService.class);

    private final GitService gitService;
    private final ChatModel chatModel;
    private final BufferedReader reader;

    /**
     * Constructs a new {@code CommitService} instance, initializing its dependencies.
     * <p>
     * This constructor ensures the {@code CommitService} is properly set up by
     * associating it with a {@link GitService} for Git operations, a {@link ChatModel}
     * for AI-driven operations, and a {@link BufferedReader} for user input handling.
     * These parameters are critical for enabling the core functionality of the service.
     * </p>
     *
     * @param gitService the {@link GitService} instance responsible for managing Git-related operations
     * @param chatModel  the {@link ChatModel} instance used for AI-generated commit message enhancements
     * @param reader     the {@link BufferedReader} instance for reading user input during interactions
     */
    public CommitService(GitService gitService, ChatModel chatModel, BufferedReader reader) {
        this.gitService = gitService;
        this.chatModel = chatModel;
        this.reader = reader;
    }

    /**
     * Initializes a {@code CommitService} instance with the required dependencies.
     * <p>
     * This constructor configures the {@code CommitService} by associating it with a
     * {@link GitService} for handling Git-related operations and a {@link ChatModel}
     * for leveraging AI capabilities. A {@link BufferedReader} is also set up to
     * facilitate user input interactions from the console.
     * </p>
     *
     * @param gitService the {@link GitService} instance responsible for managing Git operations
     * @param chatModel  the {@link ChatModel} instance enabling AI-driven enhancements
     */
    public CommitService(GitService gitService, ChatModel chatModel) {
        this.gitService = gitService;
        this.chatModel = chatModel;
        this.reader = new BufferedReader(new InputStreamReader(System.in));
    }

    /**
     * Generates and commits changes with the assistance of AI for creating commit messages.
     * <p>
     * This method performs the following steps:
     * <ul>
     *   <li>Checks whether there are staged changes in the Git repository and throws an exception if none are found.</li>
     *   <li>Generates an AI-driven commit message based on the staged diff.</li>
     *   <li>If auto-commit is enabled, automatically commits the changes without user interaction.</li>
     *   <li>If auto-commit is disabled, displays the generated message and prompts the user for an action (accept, regenerate, edit, or cancel).</li>
     *   <li>Processes user input to either commit the changes, regenerate, edit the message, or abort the commit process.</li>
     *   <li>If auto-push is enabled, automatically pushes changes to the remote repository after a successful commit.</li>
     * </ul>
     * The commit process is only completed when the user confirms the commit, provides an edited commit message,
     * or when auto-commit successfully completes.
     * </p>
     *
     * @throws IllegalStateException if no staged changes are found in the repository.
     */
    public void generateAndCommit() {
        log.info("Checking for staged changes...");
        if (!gitService.hasStagedChanges()) {
            throw new IllegalStateException("No staged changes found! Use the 'git add' to stage changes first.");
        }

        String diff = gitService.getStagedDiff();
        boolean autoCommit = UserPreferences.isAutoCommitEnabled();
        boolean autoPush = UserPreferences.isAutoPushEnabled();

        if (autoCommit) {
            log.info("Auto-commit is enabled. Generating commit message.");
            handleAutoCommit(diff, autoPush);
        } else {
            handleInteractiveCommit(diff, autoPush);
        }
    }

    /**
     * Handles the process of automatically generating, committing, and optionally pushing changes based on a provided diff.
     * <p>
     * This method performs the following operations:
     * <ul>
     *   <li>Generates an AI-driven commit message using the given diff.</li>
     *   <li>Commits changes to the Git repository with the generated message.</li>
     *   <li>Optionally pushes the changes to the remote repository if auto-push is enabled.</li>
     *   <li>Logs success or failure of each step in the process.</li>
     * </ul>
     * In the event of a failure during message generation or commit, an appropriate error is logged
     * and the exception is rethrown to ensure proper handling by the caller.
     * </p>
     *
     * @param diff     a string representation of the changes (diff) used to generate the commit message.
     * @param autoPush a boolean flag indicating whether changes should be pushed automatically
     *                 to the remote repository after committing. If {@code true}, auto-push is enabled.
     */
    private void handleAutoCommit(String diff, boolean autoPush) {
        try {
            log.info("Generating commit message with AI...");
            String commitMessage = generateMessage(diff);
            displayMessage(commitMessage);

            System.out.println("\nAuto-committing...");

            String output = gitService.commit(commitMessage);
            displayGitOutput(output);
            log.info("Committed changes successfully.");

            handleAutoPush(autoPush);
        } catch (RuntimeException e) {
            System.err.println("\nFailed to generate commit message: " + e.getMessage());
            System.err.println("Auto-commit aborted. No changes were committed.");
            log.error("Auto-commit failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Handles the interactive commit process, allowing the user to review, regenerate,
     * edit, or cancel the commit message before committing changes.
     * <p>
     * This method performs the following functionalities:
     * <ul>
     *   <li>Generates an initial commit message based on the provided diff.</li>
     *   <li>Prompts the user to accept the message, regenerate it, edit it, or cancel the commit.</li>
     *   <li>Commits the changes if the user accepts or edits the commit message.</li>
     *   <li>Handles optional automatic pushing of changes upon successful commit if the autoPush flag is enabled.</li>
     * </ul>
     * The process continues to loop until the user confirms the commit or chooses to cancel.
     * </p>
     *
     * @param diff     the string representation of the diff used for generating the initial commit message.
     * @param autoPush a boolean value indicating whether to automatically push changes
     *                 to the remote repository after a successful commit. If {@code true},
     *                 automatic push is attempted; otherwise, it is skipped.
     */
    private void handleInteractiveCommit(String diff, boolean autoPush) {
        String commitMessage = null;
        boolean isCommitted = false;

        while (!isCommitted) {
            if (commitMessage == null) {
                log.info("Generating commit message with AI...");
                commitMessage = generateMessage(diff);
            }

            displayMessage(commitMessage);

            String choice = promptUser();

            switch (choice) {
                case "y":
                    log.info("Committing changes...");
                    String output = gitService.commit(commitMessage);

                    displayGitOutput(output);
                    log.info("Committed changes successfully.");
                    isCommitted = true;

                    handleAutoPush(autoPush);
                    break;

                case "r":
                    log.info("\nRegenerating commit message...");
                    commitMessage = null;
                    break;

                case "e":
                    commitMessage = editCommitMessage(commitMessage);
                    if (commitMessage == null) {
                        System.out.println("Edit cancelled.");
                        isCommitted = true;
                        break;
                    }

                    if (!commitMessage.isEmpty()) {
                        log.info("Using edited message as commit message...");
                        String editOutput = gitService.commit(commitMessage);
                        displayGitOutput(editOutput);
                        log.info("Committed changes successfully.");
                        isCommitted = true;

                        handleAutoPush(autoPush);
                    }
                    break;

                case "c":
                    System.out.println();
                    System.out.println("Cancelling commit...");
                    isCommitted = true; //Just to break out of the while loop
                    break;

                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    /**
     * Handles the process of automatically pushing changes to the remote Git repository
     * based on the provided autoPush flag.
     * <p>
     * When {@code autoPush} is {@code true}, the method attempts to push changes using the
     * {@code gitService.push()} method. The output of the push operation is displayed via the
     * {@code displayGitOutput} method. If the push operation fails, an error message is logged and printed.
     * When {@code autoPush} is {@code false}, no action is taken, and a debug log entry is generated.
     * </p>
     *
     * @param autoPush a boolean flag indicating whether to enable or disable automatic pushing of changes.
     *                 If {@code true}, changes are pushed to the remote repository; otherwise, no action is performed.
     */
    private void handleAutoPush(boolean autoPush) {
        if (autoPush) {
            log.debug("Auto-push enabled.");
            System.out.println("\nAuto-pushing...");
            try {
                String pushOutput = gitService.push();
                displayGitOutput(pushOutput);
                log.info("Pushed changes successfully.");
            } catch (RuntimeException e) {
                System.err.println("\nFailed to push: " + e.getMessage());
                System.err.println("Commit was successful, but push failed.");
                log.error("Push failed: {}", e.getMessage(), e);
            }
        } else {
            log.debug("Auto-push is disabled.");
        }
    }

    /**
     * Prompts the user for input regarding the next action to take with the provided commit message.
     * <p>
     * This method displays a set of options to the user: confirm the commit message (yes), regenerate,
     * edit, or cancel. If the user provides no input or an invalid input, the default action is
     * considered as "yes". In case of an error during input reading, the method also defaults to "yes".
     * </p>
     *
     * @return the user's choice as a lowercase string: "y", "r", "e", or "c". Defaults to "y" if no input
     * is provided or an IOException occurs.
     */
    private String promptUser() {
        System.out.println();
        System.out.println("Commit with this message? (y)es / (r)egenerate / (e)dit / (c)ancel [y]: ");
        System.out.flush();

        try {
            String input = reader.readLine();
            if (input == null || input.isBlank() || input.trim().isEmpty()) {
                return "y";
            }
            return input.trim().toLowerCase();
        } catch (IOException e) {
            log.error("Failed to read user input: {}", e.getMessage());
            return "y";
        }
    }

    /**
     * Allows the user to edit a commit message interactively through the console.
     * <p>
     * This method displays the current commit message to the user and prompts them to either enter
     * a new message or press Enter to keep the original message unchanged. If an input error occurs
     * or the user does not provide a valid new message, the original message is returned by default.
     * </p>
     *
     * @param originalMessage the original commit message to be potentially modified by the user
     * @return the new commit message provided by the user, or the original message if no valid input is given
     */
    private String editCommitMessage(String originalMessage) {
        System.out.println();
        System.out.println("=".repeat(60));
        System.out.println("Current message:");
        System.out.println(originalMessage);
        System.out.println("=".repeat(60));
        System.out.println();
        System.out.println("Enter new commit message or press Enter to keep current one:");
        System.out.println("> ");
        System.out.flush();

        try {
            String newMessage = reader.readLine();
            if (newMessage == null || newMessage.trim().isEmpty()) {
                System.out.println("Empty message. Using original commit message.");
                return originalMessage;
            }

            return newMessage.trim();
        } catch (IOException e) {
            log.error("Failed to read user input: {}", e.getMessage());
            System.out.println("Error reading user input. Using original commit message.");
            return originalMessage;
        }
    }

    /**
     * Generates an AI-crafted message based on the given diff input.
     * This method utilizes a chat model to process the provided diff and create a meaningful, concise message.
     * If the generated message is null, empty, or if an error occurs during message generation, appropriate exceptions are thrown.
     *
     * @param diff the input string representing the diff
     */
    private String generateMessage(String diff) {
        try {
            Prompt prompt = new Prompt(
                    List.of(
                            new SystemMessage(SYSTEM_PROMPT),
                            new UserMessage(diff)
                    )
            );

            String message = chatModel.call(prompt)
                                      .getResult()
                                      .getOutput()
                                      .getText();

            if (message == null || message.isBlank() || message.trim().isEmpty()) {
                throw new IllegalStateException("AI returned an empty commit message");
            }

            return message.trim();
        } catch (Exception e) {
            log.error("Failed to generate a commit message: {}", e.getMessage(), e);

            if (e instanceof IllegalStateException) {
                throw new IllegalStateException("AI returned an empty commit message");
            }

            Throwable cause = e.getCause();
            while (cause != null) {
                if (cause instanceof ConnectException) {
                    throw new RuntimeException(
                            "Cannot connect to AI provider. Check your internet connection and verify the provider is accessible.",
                            e
                    );
                }
                cause = cause.getCause();
            }

            throw new RuntimeException("Failed to generate a commit message: " + e.getMessage(), e);
        }
    }

    /**
     * Displays the provided AI-generated commit message in a formatted manner to the console.
     * This method includes a header, footer, and blank line for better readability.
     *
     * @param message the AI-generated commit message to display
     */
    private void displayMessage(String message) {
        System.out.println();
        System.out.println("AI generated commit message:");
        System.out.println("-".repeat(60));
        System.out.println(message);
        System.out.println("-".repeat(60));
    }

    /**
     * Displays the output of a Git command to the console.
     * This method adds a blank line before printing the given output,
     * ensuring better readability in the console.
     *
     * @param output the string output of a Git command to be displayed in the console
     */
    private void displayGitOutput(String output) {
        System.out.println();
        System.out.println(output);
    }
}
