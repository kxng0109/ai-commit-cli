package service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

import java.net.ConnectException;

/**
 * The CommitService class facilitates the generation and execution of commit
 * messages for staged changes in a Git repository. It leverages an AI-based
 * service to create commit messages that adhere to standards, providing a
 * streamlined approach for developers to commit changes efficiently.
 */
public class CommitService {

    private static final Logger log = LoggerFactory.getLogger(CommitService.class);

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
            
            Input Data:
            
            {diff}
            """;

    private final GitService gitService;
    private final ChatClient chatClient;

    public CommitService(GitService gitService, ChatClient chatClient) {
        this.gitService = gitService;
        this.chatClient = chatClient;
    }

    /**
     * Automatically generates a commit message for the staged changes using an AI-backed service
     * and commits the changes to the Git repository.
     *
     * This method checks if there are any staged changes. If no staged changes are found,
     * it throws an exception prompting the user to stage changes first. When staged changes are found,
     * it retrieves the diff of those changes, generates a commit message using an AI service, displays
     * the generated message, and performs the commit operation with the generated message.
     * If the commit is successful, the Git's command output is displayed.
     *
     * @throws IllegalStateException if there are no staged changes in the repository or if the AI service
     *                               fails to generate a valid commit message.
     * @throws RuntimeException if connectivity issues or errors occur during the interaction with the AI service.
     */
    public void generateAndCommit() {
        log.info("Checking for staged changes...");
        if(!gitService.hasStagedChanges()){
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
     * Generates a commit message based on the provided diff by communicating with the AI service.
     * If the AI service fails to provide a valid commit message or encounters connectivity issues,
     * appropriate exceptions are thrown.
     *
     * @param diff the staged changes or differences as a string input for the AI to analyze
     * @return a generated commit message as a string, trimmed of whitespace
     * @throws IllegalStateException if the AI returns an empty or blank commit message
     * @throws RuntimeException if any connectivity issues or errors occur during message generation
     */
    private String generateMessage(String diff) {
        try {
            String message = chatClient.prompt()
                                       .system(SYSTEM_PROMPT)
                                       .user(diff)
                                       .call()
                                       .content();

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
                            "Cannot connect to AI provider. Check your internet connection and verify the provider is accessible.", e
                    );
                }
                cause = cause.getCause();
            }

            throw new RuntimeException("Failed to generate a commit message: " + e.getMessage(), e);
        }
    }

    /**
     * Displays the provided message in a formatted style to the console.
     * This includes a header, a separator line, and the message content.
     *
     * @param message the message to be displayed in the console
     */
    private void displayMessage(String message) {
        System.out.println();
        System.out.println("AI generated commit message:");
        System.out.println("—".repeat(60));
        System.out.println(message);
        System.out.println("—".repeat(60));
    }

    /**
     * Displays the provided Git output to the console.
     *
     * @param output the output generated from a Git operation to be displayed
     */
    private void displayGitOutput(String output){
        System.out.println();
        System.out.println(output);
    }
}
