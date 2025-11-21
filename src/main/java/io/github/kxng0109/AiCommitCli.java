package io.github.kxng0109;

import io.github.kxng0109.config.Config;
import io.github.kxng0109.service.AiProviderFactory;
import io.github.kxng0109.service.CommitService;
import io.github.kxng0109.service.GitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * The {@code AiCommitCli} class serves as the entry point for the AI Commit command-line interface.
 * This utility automates the generation of conventional commit messages using AI-based providers
 * such as OpenAI, Google Gemini, or Ollama. It provides configurable options for customization,
 * help instructions, logging, and version information.
 *
 * <p>Main functionality includes:</p>
 * <ul>
 *   <li>Displaying version information using the {@code --version} or {@code -v} argument.</li>
 *   <li>Providing usage instructions and detailed help using the {@code --help} or {@code -h} argument.</li>
 *   <li>Configuring logging behavior through environment variables.</li>
 *   <li>Loading configurations, integrating with AI providers, and enabling users to auto-generate
 *       and commit messages using AI.</li>
 *   <li>Handling potential errors during execution and logging for enhanced traceability.</li>
 * </ul>
 *
 * <p>This tool simplifies the commit message creation workflow for developers by automating the
 * process via AI-backed recommendations while allowing complete flexibility with various AI providers.</p>
 *
 * <p>For more help or usage instructions, invoke the tool with the {@code -h} or {@code --help} flag.</p>
 *
 * @version The CLI version is dynamically loaded at runtime from a {@code version.properties} file,
 * defaulting to {@code "unknown"} if the file is unavailable.
 * @see #main(String[])
 * @see #loadVersion()
 * @see #configureLogging()
 * @see #printHelp()
 */
public class AiCommitCli {
    static {
        configureLogging();
    }

    private static final Logger log = LoggerFactory.getLogger(AiCommitCli.class);
    private static final String VERSION = loadVersion();

    public static void main(String[] args) {
        if (args.length > 0) {
            String arg = args[0];
            if (arg.equals("--version") || arg.equals("-v")) {
                System.out.println("ai-commit version " + VERSION);
                System.exit(0);
            }

            if (arg.equals("--help") || arg.equals("-h")) {
                printHelp();
                System.exit(0);
            }
        }

        try {
            Config config = Config.loadFromEnv();

            GitService gitService = new GitService(config.commandTimeoutSeconds());
            ChatModel chatModel = AiProviderFactory.createChatModel(config);
            CommitService commitService = new CommitService(gitService, chatModel);

            commitService.generateAndCommit();
            System.exit(0);
        } catch (IllegalStateException e) {
            System.err.println("\nAn error occurred: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            log.error("Unexpected error occurred: {}", e.getMessage(), e);
            System.err.println("\nAn error occurred: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Loads the version information of the application from the {@code version.properties} file.
     * <p>
     * This method attempts to read the {@code version} property from a {@code version.properties}
     * file located in the classpath. If the file is unavailable or an error occurs during
     * reading, it will return a default value of {@code "unknown"}.
     * </p>
     *
     * @return the application version as a {@link String}, or {@code "unknown"} if the version
     * cannot be determined.
     */
    private static String loadVersion() {
        try (InputStream input = AiCommitCli.class.getClassLoader()
                                                  .getResourceAsStream("version.properties")) {
            if (input != null) {
                Properties props = new Properties();
                props.load(input);
                return props.getProperty("version", "unknown");
            }
        } catch (IOException e) {
            log.debug("Could not load version from properties", e);
        }
        return "unknown";
    }

    /**
     * Configures logging settings for the application.
     * <p>
     * This method initializes logging levels and behavior for the application
     * by setting properties for the Simple SLF4J Logger. It ensures concise and
     * consistent log output depending on the configured log level.
     * </p>
     * <ul>
     *   <li>The log level can be customized via the {@code AI_LOG_LEVEL} environment variable
     *       (e.g., ERROR, WARN, INFO, DEBUG). If unspecified, it defaults to {@code WARN}.</li>
     *   <li>Hides unnecessary details such as timestamps, thread names, and logger names
     *       to maintain clean log output.</li>
     * </ul>
     */
    private static void configureLogging() {
        String logLevel = System.getenv().getOrDefault("AI_LOG_LEVEL", "WARN");
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", logLevel);
        System.setProperty("org.slf4j.simpleLogger.showDateTime", "false");
        System.setProperty("org.slf4j.simpleLogger.showThreadName", "false");
        System.setProperty("org.slf4j.simpleLogger.showLogName", "false");
    }

    /**
     * Prints the help information and usage instructions for the AI Commit CLI tool.
     * This method provides a detailed guide on available options, environment variable
     * configurations, and usage examples for various AI providers. The priority order
     * for provider selection is also included.
     * <p>
     * The printed output includes:
     * <ul>
     *   <li>General description of AI Commit functionality.</li>
     *   <li>Command-line options and their usage.</li>
     *   <li>Supported AI providers and required configuration environment variables.</li>
     *   <li>Optional settings for AI behavior and logging.</li>
     *   <li>Usage examples for setting up and using AI Commit with different providers.</li>
     *   <li>Provider priority order in case multiple are configured.</li>
     * </ul>
     * <p>
     * This method is typically invoked when the user supplies the {@code -h} or {@code --help} option.
     */
    private static void printHelp() {
        System.out.println("""
                                   AI Commit - Generate conventional commit messages using AI
                                   
                                   USAGE:
                                       ai-commit [OPTIONS]
                                   
                                   OPTIONS:
                                       -h, --help       Show this help message
                                       -v, --version    Show version information
                                   
                                   ENVIRONMENT VARIABLES:
                                   
                                       AI Provider (choose ONE):
                                   
                                       1. OpenAI / OpenAI-compatible APIs (OpenRouter, Together, etc.):
                                          OPENAI_API_KEY           Your API key (required)
                                          OPENAI_MODEL             Model name (default: gpt-4o-mini)
                                          OPENAI_BASE_URL          API endpoint (default: https://api.openai.com)
                                   
                                       2. Anthropic:
                                          ANTHROPIC_API_KEY           Your API key (required)
                                          ANTHROPIC_MODEL             Model name (default: claude-sonnet-4-0)
                                   
                                       3. Google Gemini:
                                          GOOGLE_API_KEY           Your API key (required)
                                          GOOGLE_MODEL             Model name (default: gemini-2.0-flash)
                                   
                                       4. Deepseek:
                                          DEEPSEEK_API_KEY           Your API key (required)
                                   
                                       5. Ollama (local models):
                                          OLLAMA_MODEL             Model name (required, e.g., llama3, qwen2.5)
                                          OLLAMA_BASE_URL          Server URL (default: http://localhost:11434)
                                   
                                       Optional Settings:
                                          AI_LOG_LEVEL             Log level: ERROR, WARN, INFO, DEBUG (default: WARN)
                                          AI_TEMPERATURE           Model temperature 0.0-2.0 (default: 0.1)
                                          AI_COMMAND_TIMEOUT       Git command timeout seconds (default: 30)
                                   
                                   EXAMPLES:
                                   
                                       # Using OpenAI
                                       export OPENAI_API_KEY="sk-..."
                                       git add .
                                       ai-commit
                                   
                                       # Using Ollama locally
                                       export OLLAMA_MODEL="llama3"
                                       git add .
                                       ai-commit
                                   
                                       # Using OpenRouter with Claude
                                       export OPENAI_API_KEY="sk-or-..."
                                       export OPENAI_BASE_URL="https://openrouter.ai/api/v1"
                                       export OPENAI_MODEL="anthropic/claude-3.5-sonnet"
                                       git add .
                                       ai-commit
                                   
                                       # Using Google Gemini
                                       export GOOGLE_API_KEY="AIza..."
                                       git add .
                                       ai-commit
                                   
                                   PRIORITY ORDER:
                                       If multiple providers are configured, priority is: OpenAI > Anthropic > Google > Deepseek > Ollama
                                   
                                   For more information: https://github.com/kxng0109/ai-commit-cli
                                   """);
    }
}
