package io.github.kxng0109.service;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards for secret environment scrubbing in {@link GitService}.
 */
@DisplayName("GitService env scrub")
public class GitServiceEnvTest {

    @Test
    @DisplayName("secret entries are removed, operational entries kept")
    void secrets_areRemovedOperationalKept() {
        Map<String, String> environment = new HashMap<>();
        environment.put("OPENAI_API_KEY", "sk-secret");
        environment.put("ANTHROPIC_API_KEY", "sk-ant-secret");
        environment.put("AI_TEMPERATURE", "0.1");
        environment.put("AI_LOG_LEVEL", "DEBUG");
        environment.put("PATH", "/usr/bin");
        environment.put("GIT_PAGER", "cat");

        GitService.scrubSecretEnv(environment);

        assertThat(environment)
                .doesNotContainKeys("OPENAI_API_KEY", "ANTHROPIC_API_KEY", "AI_TEMPERATURE", "AI_LOG_LEVEL")
                .containsKeys("PATH", "GIT_PAGER");
    }

    @Test
    @DisplayName("similar but safe names are kept")
    void safeNames_areKept() {
        Map<String, String> environment = new HashMap<>();
        environment.put("MY_API_KEYS_BACKUP", "value");
        environment.put("OPENAI_BASE_URL", "https://api.openai.com");

        GitService.scrubSecretEnv(environment);

        assertThat(environment).containsKeys("MY_API_KEYS_BACKUP", "OPENAI_BASE_URL");
    }
}
