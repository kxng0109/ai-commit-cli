package io.github.kxng0109.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;

import java.net.ConnectException;
import java.util.List;

/**
 * The {@code CommitService} class provides functionality to automate the process of generating
 * and committing a high-quality, AI-assisted Git commit message for staged changes in a repository.
 * <p>
 * This service integrates with a given {@link GitService} for Git operations and a {@link ChatModel}
 * for AI-driven commit message generation. It ensures that commit messages adhere to standardized formats
 * and enhances productivity while maintaining consistency.
 * </p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *   <li>Validates the presence of staged changes before commit generation.</li>
 *   <li>Uses AI to craft precise, professional commit messages based on diff details.</li>
 *   <li>Displays the generated commit message for user visibility.</li>
 *   <li>Handles errors such as absent staged changes or AI service connectivity issues gracefully.</li>
 * </ul>
 *
 * <h2>Usage:</h2>
 * Instantiate the {@code CommitService} with the required {@link GitService} and {@link ChatModel} dependencies.
 * Call {@link #generateAndCommit()} to automate the commit message creation and commit process.
 *
 * <h2>Exceptions:</h2>
 * <ul>
 *   <li>{@link IllegalStateException} if no staged changes are available or the AI generates an empty message.</li>
 *   <li>{@link RuntimeException} for other errors such as connectivity issues with the AI provider.</li>
 * </ul>
 *
 * <h2>Dependencies:</h2>
 * This class relies on:
 * <ul>
 *   <li>{@link GitService} for handling Git commands.</li>
 *   <li>{@link ChatModel} for generating AI-based commit messages.</li>
 *   <li>A logging framework ({@link Logger}) for status updates and error tracking.</li>
 * </ul>
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

    public CommitService(GitService gitService, ChatModel chatModel) {
        this.gitService = gitService;
        this.chatModel = chatModel;
    }

    /**
     * Automates the process of generating an AI-assisted Git commit message and committing staged changes.
     * <p>
     * This method checks for existing staged changes in the repository. If no changes are staged, it throws
     * an {@link IllegalStateException}. Upon detecting staged changes, it fetches the diff details, uses an
     * AI model to generate a descriptive commit message, displays the generated message, and commits the changes
     * with the generated message. The output of the Git commit command is then displayed on the console.
     * </p>
     *
     * @throws IllegalStateException if there are no staged changes available
     */
    public void generateAndCommit() {
        log.info("Checking for staged changes...");
        if (!gitService.hasStagedChanges()) {
            throw new IllegalStateException("No staged changes found! Use the 'git add' to stage changes first.");
        }

        String diff = gitService.getStagedDiff();

        log.info("Generating commit message with AI...");
        String commitMessage = generateMessage(diff);

        displayMessage(commitMessage);

        log.info("Committing changes...");
        String output = gitService.commit(commitMessage);

        displayGitOutput(output);
        log.info("Committed changes successfully.");
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

            if (message == null || message.isBlank()) {
                throw new IllegalStateException("AI returned an empty commit message");
            }

            return message.trim();
        } catch (Exception e) {
            log.error("Failed to generate a commit message: {}", e.getMessage(), e);

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
