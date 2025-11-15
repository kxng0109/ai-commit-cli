package io.github.kxng0109;

import io.github.kxng0109.service.AiProviderFactory;
import io.github.kxng0109.service.CommitService;
import io.github.kxng0109.config.Config;
import io.github.kxng0109.service.GitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

/**
 * The io.github.kxng0109.AiCommitCli class serves as the main entry point for the AI Commit CLI tool,
 * which generates conventional commit messages using AI implementations from
 * various providers such as OpenAI, Google Gemini, Ollama and more to come.
 *
 * This class facilitates interaction with the command-line interface, allowing
 * users to configure, execute, and manage AI-driven commit message generation
 * within a Git-compatible environment.
 *
 * Features:
 * - Supports multiple AI providers, configurable via environment variables.
 * - Generates meaningful commit messages from Git changes using AI.
 * - Configures logging levels dynamically through environment variables.
 * - Offers a help menu and version details for user guidance.
 *
 * The main method processes arguments, sets up configurations, initializes
 * services, and handles any operational exceptions encountered during execution.
 */
public class AiCommitCli {
    private static final Logger log = LoggerFactory.getLogger(AiCommitCli.class);
    private static final String VERSION = "1.0.0"; //Search for if there's a way to get this from the pom.xml instead of hardcoding it

    public static void main(String[] args) {
        if(args.length > 0){
            String arg = args[0];
            if(arg.equals("--version") || arg.equals("-v")){
                System.out.println("ai-commit version " + VERSION);
                System.exit(0);
            }

            if(arg.equals("--help") || arg.equals("-h")){
                printHelp();
                System.exit(0);
            }
        }

        String logLevel = System.getenv().getOrDefault("AI_LOG_LEVEL", "WARN");
        System.setProperty("org.slf4j.simple.defaultLogLevel", logLevel);
        System.setProperty("org.slf4j.simple.showDateTime", "false");
        System.setProperty("org.slf4j.simple.showThreadName", "false");
        System.setProperty("org.slf4j.simple.showLogName", "false");

        try {
            Config config = Config.loadFromEnv();

            GitService gitService = new GitService(config.commandTimeoutSeconds());
            ChatClient chatClient = AiProviderFactory.createChatClient(config);
            CommitService commitService = new CommitService(gitService, chatClient);

            commitService.generateAndCommit();
            System.exit(0);
        }catch (IllegalStateException e){
            System.err.println("\nAn error occurred: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            log.error("Unexpected error occurred: {}", e.getMessage(), e);
            System.err.println("\nAn error occurred: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Prints the help message for the AI Commit CLI tool, providing information
     * about usage, available options, environment variables, examples, and
     * priority order for multiple AI providers. This method displays detailed
     * guidance on configuring and using different AI backends supported by the
     * tool, as well as instructions for setting optional parameters.
     *
     * The help message includes the following sections:
     * - General tool description
     * - Usage syntax and available options
     * - Required and optional environment variables for AI provider configuration
     * - Examples for configuring and invoking the tool with specific providers
     * - Priority rules when multiple providers are configured
     * - Link for further documentation
     */
    private static void printHelp(){
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
                   OPENAI_MODEL             Model name (default: gpt-4o)
                   OPENAI_BASE_URL          API endpoint (default: https://api.openai.com)
                
                2. Google Gemini:
                   GOOGLE_API_KEY           Your API key (required)
                   GOOGLE_MODEL             Model name (default: gemini-2.0-flash-exp)
                
                3. Ollama (local models):
                   OLLAMA_MODEL             Model name (required, e.g., llama3, qwen2.5)
                   OLLAMA_BASE_URL          Server URL (default: http://localhost:11434)
                
                Optional Settings:
                   AI_LOG_LEVEL             Log level: ERROR, WARN, INFO, DEBUG (default: WARN)
                   AI_TEMPERATURE           Model temperature 0.0-1.0 (default: 0.1)
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
                export OPENAI_BASE_URL="https://openrouter.ai/api/"
                export OPENAI_MODEL="anthropic/claude-sonnet-4.5
                git add .
                ai-commit
                
                # Using Google Gemini
                export GOOGLE_API_KEY="AIza..."
                git add .
                ai-commit
                
                Note: Use "set" for command prompt and "$env:" for PowerShell instead of "export" if you are using windows

            PRIORITY ORDER:
                If multiple providers are configured, priority is: OpenAI > Google > Ollama
            
            For more information: https://github.com/kxng0109/ai-commit-cli
            """);
    }
}
