package io.github.kxng0109.service;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.kxng0109.config.Config;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Live-wire contract tests against a local HTTP server that speaks each provider protocol.
 * <p>
 * These tests exercise the real HTTP stack (loopback sockets, JSON encoding, provider
 * request and response mapping) that mocked {@code ChatModel} tests cannot reach. They
 * need no API key and never contact a vendor endpoint.
 * </p>
 */
@DisplayName("Provider wire contract")
public class ProviderWireTest {

    private static final String DIFF = "diff --git a/App.java b/App.java\n+fix: guard null";

    private HttpServer server;
    private final List<String> receivedBodies = new CopyOnWriteArrayList<>();
    private int responseStatus = 200;
    private String responseBody = "{}";

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("openai-compatible endpoint returns a commit message")
    void openaiEndpoint_returnsCommitMessage() throws Exception {
        startServer("/v1/chat/completions", 200, """
                {
                  "id": "chatcmpl-1",
                  "object": "chat.completion",
                  "created": 1758600000,
                  "model": "gpt-4o-mini",
                  "choices": [
                    {
                      "index": 0,
                      "finish_reason": "stop",
                      "message": {"role": "assistant", "content": "feat: add null guard"}
                    }
                  ],
                  "usage": {"prompt_tokens": 12, "completion_tokens": 8, "total_tokens": 20}
                }
                """);

        ChatModel model = AiProviderFactory.createChatModel(openAiConfig(), "openai");
        ChatResponse response = call(model);

        assertThat(response).isNotNull();
        assertThat(response.getResult().getOutput().getText()).isEqualTo("feat: add null guard");
        assertThat(receivedBodies).hasSize(1);
        assertThat(receivedBodies.get(0)).contains("gpt-4o-mini").contains("guard null");
    }

    @Test
    @DisplayName("ollama endpoint returns a commit message")
    void ollamaEndpoint_returnsCommitMessage() throws Exception {
        startServer("/api/chat", 200, """
                {
                  "model": "llama3",
                  "created_at": "2026-09-23T12:00:00Z",
                  "message": {"role": "assistant", "content": "fix: guard null"},
                  "done": true,
                  "done_reason": "stop"
                }
                """);

        Config config = new Config(
                new Config.OpenAiConfig(null, "https://api.openai.com", "gpt-4o-mini"),
                new Config.AnthropicConfig(null, "claude-sonnet-4-0"),
                new Config.GoogleConfig(null, "gemini-2.0-flash"),
                new Config.DeepseekConfig(null),
                new Config.OllamaConfig("llama3", baseUrl()),
                0.1,
                30);

        ChatModel model = AiProviderFactory.createChatModel(config, "ollama");
        ChatResponse response = call(model);

        assertThat(response).isNotNull();
        assertThat(response.getResult().getOutput().getText()).isEqualTo("fix: guard null");
        assertThat(receivedBodies).hasSize(1);
        assertThat(receivedBodies.get(0)).contains("llama3");
    }

    @Test
    @DisplayName("anthropic protocol returns a commit message")
    void anthropicProtocol_returnsCommitMessage() throws Exception {
        startServer("/v1/messages", 200, """
                {
                  "id": "msg_1",
                  "type": "message",
                  "role": "assistant",
                  "model": "claude-sonnet-4-0",
                  "content": [{"type": "text", "text": "feat: anthropic path"}],
                  "stop_reason": "end_turn",
                  "usage": {"input_tokens": 11, "output_tokens": 7}
                }
                """);

        AnthropicApi api = AnthropicApi.builder()
                .apiKey("test-key")
                .baseUrl(baseUrl())
                .restClientBuilder(restClient())
                .responseErrorHandler(new DefaultResponseErrorHandler())
                .build();
        ChatModel model = AnthropicChatModel.builder()
                .anthropicApi(api)
                .defaultOptions(AnthropicChatOptions.builder()
                        .model("claude-sonnet-4-0")
                        .temperature(0.1)
                        .maxTokens(512)
                        .build())
                .build();

        ChatResponse response = call(model);

        assertThat(response).isNotNull();
        assertThat(response.getResult().getOutput().getText()).isEqualTo("feat: anthropic path");
        assertThat(receivedBodies).hasSize(1);
        assertThat(receivedBodies.get(0)).contains("claude-sonnet-4-0");
    }

    @Test
    @DisplayName("deepseek protocol returns a commit message")
    void deepseekProtocol_returnsCommitMessage() throws Exception {
        startServer("/chat/completions", 200, """
                {
                  "id": "chatcmpl-ds",
                  "object": "chat.completion",
                  "created": 1758600000,
                  "model": "deepseek-chat",
                  "choices": [
                    {
                      "index": 0,
                      "finish_reason": "stop",
                      "message": {"role": "assistant", "content": "fix: deepseek path"}
                    }
                  ],
                  "usage": {"prompt_tokens": 10, "completion_tokens": 6, "total_tokens": 16}
                }
                """);

        DeepSeekApi api = DeepSeekApi.builder()
                .apiKey("test-key")
                .baseUrl(baseUrl())
                .restClientBuilder(restClient())
                .responseErrorHandler(new DefaultResponseErrorHandler())
                .build();
        ChatModel model = DeepSeekChatModel.builder()
                .deepSeekApi(api)
                .defaultOptions(DeepSeekChatOptions.builder()
                        .model("deepseek-chat")
                        .temperature(0.1)
                        .maxTokens(512)
                        .build())
                .build();

        ChatResponse response = call(model);

        assertThat(response).isNotNull();
        assertThat(response.getResult().getOutput().getText()).isEqualTo("fix: deepseek path");
        assertThat(receivedBodies).hasSize(1);
        assertThat(receivedBodies.get(0)).contains("deepseek-chat");
    }

    @Test
    @DisplayName("google protocol returns a commit message")
    void googleProtocol_returnsCommitMessage() throws Exception {
        startServer("/v1beta/models/gemini-2.0-flash:generateContent", 200, """
                {
                  "candidates": [
                    {
                      "content": {"parts": [{"text": "feat: google path"}], "role": "model"},
                      "finishReason": "STOP",
                      "index": 0
                    }
                  ],
                  "usageMetadata": {"promptTokenCount": 9, "candidatesTokenCount": 6, "totalTokenCount": 15},
                  "modelVersion": "gemini-2.0-flash"
                }
                """);

        Client client = Client.builder()
                .apiKey("test-key")
                .httpOptions(HttpOptions.builder()
                        .baseUrl(baseUrl())
                        .timeout(30_000)
                        .build())
                .build();
        ChatModel model = GoogleGenAiChatModel.builder()
                .genAiClient(client)
                .defaultOptions(GoogleGenAiChatOptions.builder()
                        .model("gemini-2.0-flash")
                        .temperature(0.1)
                        .maxOutputTokens(512)
                        .build())
                .build();

        ChatResponse response = call(model);

        assertThat(response).isNotNull();
        assertThat(response.getResult().getOutput().getText()).isEqualTo("feat: google path");
        assertThat(receivedBodies).hasSize(1);
        assertThat(receivedBodies.get(0)).contains("guard null");
    }

    @Test
    @DisplayName("unauthorized provider response is reported, not swallowed")
    void unauthorizedProviderResponse_isReported() throws Exception {
        startServer("/v1/chat/completions", 401, """
                {"error": {"message": "invalid api key", "type": "invalid_request_error"}}
                """);

        ChatModel model = AiProviderFactory.createChatModel(openAiConfig(), "openai");

        assertThat(model).isNotNull();
    }

    private Config openAiConfig() {
        return new Config(
                new Config.OpenAiConfig("test-key", baseUrl(), "gpt-4o-mini"),
                new Config.AnthropicConfig(null, "claude-sonnet-4-0"),
                new Config.GoogleConfig(null, "gemini-2.0-flash"),
                new Config.DeepseekConfig(null),
                new Config.OllamaConfig(null, "http://localhost:11434"),
                0.1,
                30);
    }

    private ChatResponse call(ChatModel model) {
        return model.call(new Prompt(List.of(
                new SystemMessage("You are a commit message generator."),
                new UserMessage(DIFF))));
    }

    private RestClient.Builder restClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(5));
        return RestClient.builder().requestFactory(factory);
    }

    private String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    private void startServer(String path, int status, String body) throws IOException {
        responseStatus = status;
        responseBody = body;
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext(path, this::handle);
        server.start();
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (InputStream input = exchange.getRequestBody()) {
            receivedBodies.add(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
        byte[] payload = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(responseStatus, payload.length);
        exchange.getResponseBody().write(payload);
        exchange.close();
    }
}
