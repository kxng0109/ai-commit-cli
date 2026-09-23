package io.github.kxng0109;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards for CLI switch parsing and error description in {@link AiCommitCli}.
 */
@DisplayName("AiCommitCli policy")
public class AiCommitCliTest {

    @Test
    @DisplayName("on forms enable the switch")
    void onForms_enableSwitch() {
        assertThat(AiCommitCli.parseToggle("on", "--auto-commit")).isTrue();
        assertThat(AiCommitCli.parseToggle("ON", "--auto-commit")).isTrue();
        assertThat(AiCommitCli.parseToggle("true", "--auto-commit")).isTrue();
        assertThat(AiCommitCli.parseToggle("1", "--auto-commit")).isTrue();
        assertThat(AiCommitCli.parseToggle("yes", "--auto-commit")).isTrue();
        assertThat(AiCommitCli.parseToggle(" on ", "--auto-commit")).isTrue();
    }

    @Test
    @DisplayName("off forms disable the switch")
    void offForms_disableSwitch() {
        assertThat(AiCommitCli.parseToggle("off", "--auto-push")).isFalse();
        assertThat(AiCommitCli.parseToggle("OFF", "--auto-push")).isFalse();
        assertThat(AiCommitCli.parseToggle("false", "--auto-push")).isFalse();
        assertThat(AiCommitCli.parseToggle("0", "--auto-push")).isFalse();
        assertThat(AiCommitCli.parseToggle("no", "--auto-push")).isFalse();
    }

    @Test
    @DisplayName("unknown switch value is rejected without state change")
    void unknownValue_isRejected() {
        assertThatThrownBy(() -> AiCommitCli.parseToggle("banana", "--auto-commit"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("on|off");
        assertThatThrownBy(() -> AiCommitCli.parseToggle("", "--auto-push"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("error description never blanks")
    void errorDescription_neverBlanks() {
        assertThat(AiCommitCli.describeError(new IllegalStateException("boom"))).isEqualTo("boom");
        assertThat(AiCommitCli.describeError(new IllegalStateException())).isNotBlank();
        assertThat(AiCommitCli.describeError(new IllegalStateException("   "))).isNotBlank();
    }
}
