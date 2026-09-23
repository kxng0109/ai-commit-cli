package io.github.kxng0109.service;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards for {@link SecretScanner} redaction and size policy.
 */
@DisplayName("SecretScanner")
public class SecretScannerTest {

    @Test
    @DisplayName("clean diff returns no findings")
    void cleanDiff_returnsNoFindings() {
        String diff = "diff --git a/App.java b/App.java\n+fix: correct null handling";

        assertThat(SecretScanner.scan(diff)).isEmpty();
    }

    @Test
    @DisplayName("null diff is rejected")
    @SuppressWarnings("DataFlowIssue")
    void nullDiff_isRejected() {
        assertThatThrownBy(() -> SecretScanner.scan(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("private key block is detected")
    void privateKey_isDetected() {
        String diff = "+-----BEGIN PRIVATE KEY-----\n+MIIEvwIBADANBgkqhkiG9w0BAQEFAASC";

        List<SecretScanner.Finding> findings = SecretScanner.scan(diff);

        assertThat(findings).extracting(SecretScanner.Finding::ruleId).contains("PRIVATE_KEY");
    }

    @Test
    @DisplayName("aws access key is detected")
    void awsAccessKey_isDetected() {
        String diff = "+AKIAIOSFODNN7EXAMPLE";

        assertThat(SecretScanner.scan(diff))
                .extracting(SecretScanner.Finding::ruleId)
                .contains("AWS_ACCESS_KEY");
    }

    @Test
    @DisplayName("github token is detected")
    void githubToken_isDetected() {
        String diff = "+ghp_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

        assertThat(SecretScanner.scan(diff))
                .extracting(SecretScanner.Finding::ruleId)
                .contains("GITHUB_TOKEN");
    }

    @Test
    @DisplayName("api key is detected")
    void apiKey_isDetected() {
        String diff = "+sk-test-abcdefghijklmnopqrst";

        assertThat(SecretScanner.scan(diff))
                .extracting(SecretScanner.Finding::ruleId)
                .contains("API_KEY");
    }

    @Test
    @DisplayName("jwt is detected")
    void jwt_isDetected() {
        String diff = "+eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0In0.SflKxwRJ";

        assertThat(SecretScanner.scan(diff))
                .extracting(SecretScanner.Finding::ruleId)
                .contains("JWT");
    }

    @Test
    @DisplayName("bearer token is detected")
    void bearerToken_isDetected() {
        String diff = "+Authorization: Bearer abcdefghijklmnopqrstuvwx";

        assertThat(SecretScanner.scan(diff))
                .extracting(SecretScanner.Finding::ruleId)
                .contains("BEARER_TOKEN");
    }

    @Test
    @DisplayName("credential assignment is detected")
    void credentialAssignment_isDetected() {
        String diff = "+password = hunter2secretvalue";

        assertThat(SecretScanner.scan(diff))
                .extracting(SecretScanner.Finding::ruleId)
                .contains("CREDENTIAL_ASSIGNMENT");
    }

    @Test
    @DisplayName("credentialed connection string is detected")
    void connectionString_isDetected() {
        String diff = "+postgres://appuser:pass123word@db.internal:5432/app";

        assertThat(SecretScanner.scan(diff))
                .extracting(SecretScanner.Finding::ruleId)
                .contains("CONNECTION_STRING");
    }

    @Test
    @DisplayName("binary content is detected")
    void binaryContent_isDetected() {
        String diff = "diff --git a/logo.png b/logo.png\n binary content \u0000 here";

        assertThat(SecretScanner.scan(diff))
                .extracting(SecretScanner.Finding::ruleId)
                .contains("BINARY");
    }

    @Test
    @DisplayName("generated lockfile is detected")
    void lockfile_isDetected() {
        String diff = "diff --git a/package-lock.json b/package-lock.json\n+++ b/package-lock.json\n+{}";

        assertThat(SecretScanner.scan(diff))
                .extracting(SecretScanner.Finding::ruleId)
                .contains("LOCKFILE");
    }

    @Test
    @DisplayName("oversize diff is blocked")
    void oversizeDiff_isBlocked() {
        String diff = "a".repeat(SecretScanner.MAX_DIFF_BYTES + 1);

        List<SecretScanner.Finding> findings = SecretScanner.scan(diff);

        assertThat(findings).extracting(SecretScanner.Finding::ruleId).containsExactly("OVERSIZE");
    }

    @Test
    @DisplayName("summary never contains secret content")
    void summary_neverContainsSecretContent() {
        String secret = "AKIAIOSFODNN7EXAMPLE";
        List<SecretScanner.Finding> findings = SecretScanner.scan("+" + secret);

        String summary = SecretScanner.summarize(findings);

        assertThat(summary).contains("AWS_ACCESS_KEY");
        assertThat(summary).doesNotContain(secret);
    }

    @Test
    @DisplayName("summary separates multiple findings")
    void summary_separatesMultipleFindings() {
        List<SecretScanner.Finding> findings = SecretScanner.scan(
                "+AKIAIOSFODNN7EXAMPLE\n+ghp_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");

        String summary = SecretScanner.summarize(findings);

        assertThat(summary).contains("AWS_ACCESS_KEY").contains("GITHUB_TOKEN").contains("; ");
    }

    @Test
    @DisplayName("findings list is unmodifiable")
    void findings_areUnmodifiable() {
        List<SecretScanner.Finding> findings = SecretScanner.scan("clean");

        assertThatThrownBy(() -> findings.add(new SecretScanner.Finding("X", "y", 1, "fp")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("utility constructor is guarded")
    void constructor_isGuarded() {
        assertThatThrownBy(() -> {
            var constructor = SecretScanner.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        }).isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(UnsupportedOperationException.class);
    }
}
