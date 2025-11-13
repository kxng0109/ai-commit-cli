import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.web.client.DefaultResponseErrorHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.slf4j.simple.SimpleLogger.DEFAULT_LOG_LEVEL_KEY;

public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    private static final String SYSTEM_MESSAGE = """
            You are an expert software engineer and a strict maintainer of a large-scale project. You are reviewing a contribution and your sole task is to write the final commit message for it.
            Analyze the provided git diff and generate a single, professional commit message that adheres perfectly to the Conventional Commits specification.
            Rules (Must be followed step-by-step):
            1. Format: The commit message MUST be structured as:
            <type>(<scope>): <description>
            
            [optional body]
            
            [optional footer]
            
            2. Type (The Prefix):
            You MUST determine the correct type by analyzing the diff's intent. The allowed types are:
            feat: (A new feature)
            fix: (A bug fix)
            refactor: (A code change that neither fixes a bug nor adds a feature)
            docs: (Documentation-only changes)
            test: (Adding missing tests or correcting existing tests)
            style: (Changes that do not affect the meaning of the code: white-space, formatting, missing semi-colons, etc.)
            chore: (Changes to the build process, auxiliary tools, or dependencies)
            
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
            
            7 Output Constraint (CRITICAL):
            Your response MUST contain only the raw, formatted commit message and nothing else.
            - DO NOT include any preamble like "Here is the commit message:".
            - DO NOT include any markdown formatting like ```.
            - The very first character of your response must be the first letter of the type.
            
            Input Data:
            
            {diff}
            """;

    private App() {
    }

    public static void main(String[] args) {
        String logLevel = System.getenv().getOrDefault("AI_COMMIT_LOG_LEVEL", "info");
        System.setProperty(DEFAULT_LOG_LEVEL_KEY, logLevel);
        log.info("Starting AI Commit Client. (Log Level: {})", logLevel);

        try {
            ChatClient chatClient = createChatClient();

            log.info("Reading staged git changes...");
            String diff = runCommand("git", "diff", "--staged");

            if (diff == null || diff.trim().isEmpty()) {
                log.error("Error: No staged changes to commit. Use 'git add' first.");
                System.exit(1);
            }

            log.info("Contacting AI provider (this may take a while)...");
            String commitMessage = chatClient.prompt()
                                             .system(SYSTEM_MESSAGE)
                                             .user(diff)
                                             .call()
                                             .content();

            if (commitMessage == null || commitMessage.trim().isEmpty()) {
                log.error("Error: AI returned an empty or null message.");
                System.exit(1);
            }

            String finalMessage = commitMessage.trim();

            System.out.println("\nAI Generated Message:");
            System.out.println("------------------------");
            System.out.println(finalMessage);
            System.out.println("------------------------");

            String commitOutput = runCommand("git", "commit", "-m", finalMessage);
            System.out.println();
            System.out.println(commitOutput);
            log.info("Commit successful.");
        } catch (Exception e) {
            log.error("An unexpected error occurred.", e);
            if (e.getCause() instanceof java.net.ConnectException) {
                log.error(
                        "Could not connect to AI provider. If using Ollama, check whether Ollama is running. Or is the base URL correct?");
            }
            System.exit(1);
        }
    }


    private static ChatClient createChatClient() {
        log.debug("Detecting AI provider...");
        String openAiKey = System.getenv("OPENAI_API_KEY");
        if (openAiKey != null && !openAiKey.trim().isEmpty()) {
            log.info("OpenAI-compatible provider detected.");

            String baseUrl = System.getenv().getOrDefault("OPENAI_BASE_URL", "https://api.openai.com");
            String model = System.getenv().getOrDefault("OPENAI_MODEL", "gpt-4o");
            log.info("Using model: {} at {}", model, baseUrl);

            OpenAiApi openAiApi = OpenAiApi.builder()
                                           .apiKey(openAiKey)
                                           .baseUrl(baseUrl)
                                           .responseErrorHandler(new DefaultResponseErrorHandler())
                                           .build();

            OpenAiChatOptions options = OpenAiChatOptions.builder()
                                                         .model(model)
                                                         .temperature(0.1)
                                                         .build();

            OpenAiChatModel chatModel = OpenAiChatModel.builder()
                                                       .openAiApi(openAiApi)
                                                       .defaultOptions(options)
                                                       .build();

            return ChatClient.builder(chatModel).build();
        }

        String ollamaModel = System.getenv("OLLAMA_MODEL");
        if (ollamaModel != null && !ollamaModel.isEmpty()) {
            log.info("Ollama provider detected.");

            String baseUrl = System.getenv().getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434");
            log.info("Using Ollama model: {} at {}", ollamaModel, baseUrl);

            OllamaApi ollamaApi = OllamaApi.builder()
                                           .baseUrl(baseUrl)
                                           .build();

            OllamaOptions options = OllamaOptions.builder()
                                                 .model(ollamaModel)
                                                 .temperature(0.1)
                                                 .build();

            OllamaChatModel chatModel = OllamaChatModel.builder()
                                                       .ollamaApi(ollamaApi)
                                                       .defaultOptions(options)
                                                       .build();

            return ChatClient.builder(chatModel).build();
        }

        throw new RuntimeException(
                """
                        Error: No AI provider configured.
                        Please set one of the following environment variable groups:
                        
                        1. For OpenAI or any compatible API (e.g., OpenRouter):
                           OPENAI_API_KEY     (your key)
                           (Optional) OPENAI_MODEL (defaults to "gpt-4o")
                           (Optional) OPENAI_BASE_URL (defaults to "[https.api.openai.com/v1](https://https.api.openai.com/v1)")
                        
                        2. For a local Ollama server:
                           OLLAMA_MODEL   (e.g., "llama3")
                           (Optional) OLLAMA_BASE_URL (defaults to "http://localhost:11434")
                        """
        );
    }

    private static String runCommand(String... command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        Process process = processBuilder.start();

        boolean finished = process.waitFor(30, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            throw new IOException("Command timed out after " + 30 + " seconds: " + String.join(" ", command));
        }

        int exitCode = process.exitValue();

        if (exitCode == 0) {
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return output.trim();
        } else {
            String errorOutput = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            throw new IOException("Command failed with exit code " + exitCode + ": " + errorOutput.trim());
        }
    }
}
