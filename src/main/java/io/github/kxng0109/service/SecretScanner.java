package io.github.kxng0109.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scans staged diffs for secrets before they are sent to external AI providers.
 * <p>
 * The scanner is dependency-free and uses standard library regular expressions
 * only. It never logs or returns secret content; findings carry rule identifiers,
 * safe hints, lengths, and one-way fingerprints instead.
 * </p>
 */
public final class SecretScanner {

    /** Maximum diff size in bytes accepted for AI submission. Larger diffs abort with instructions. */
    public static final int MAX_DIFF_BYTES = 64 * 1024;

    private static final Pattern PRIVATE_KEY =
            Pattern.compile("-----BEGIN (?:RSA |EC |OPENSSH |DSA |PGP )?PRIVATE KEY-----");
    private static final Pattern CERTIFICATE =
            Pattern.compile("-----BEGIN CERTIFICATE-----");
    private static final Pattern AWS_ACCESS_KEY = Pattern.compile("AKIA[0-9A-Z]{16}");
    private static final Pattern GITHUB_TOKEN = Pattern.compile("gh[pousr]_[A-Za-z0-9]{36,}");
    private static final Pattern OPENAI_KEY = Pattern.compile("sk-(?:live-|test-|proj-)?[A-Za-z0-9]{20,}");
    private static final Pattern JWT =
            Pattern.compile("eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+");
    private static final Pattern BEARER_TOKEN =
            Pattern.compile("(?i)Bearer\\s+[A-Za-z0-9\\-._~+/=]{20,}");
    private static final Pattern GENERIC_ASSIGNMENT = Pattern.compile(
            "(?i)(api[_-]?key|api[_-]?token|secret|client[_-]?secret|passwd|password|auth[_-]?token|access[_-]?token)\\s*(=|:|=>|:=).{1,200}");
    private static final Pattern CONNECTION_STRING = Pattern.compile(
            "(?i)(mongodb|postgres|postgresql|mysql|redis)(\\+srv)?://[^\\s]+:[^\\s]+@[^\\s]+");
    private static final Pattern ADDED_FILE =
            Pattern.compile("(?m)^\\+\\+\\+ b/(.+)$");

    private static final String[] LOCKED_SUFFIXES = {
        "package-lock.json", "pnpm-lock.yaml", "yarn.lock", "go.sum",
        "Cargo.lock", "Gemfile.lock", "poetry.lock", ".min.js", ".min.css", ".map"
    };

    private SecretScanner() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * A single blocking finding without any secret content.
     *
     * @param ruleId the stable rule identifier such as {@code PRIVATE_KEY} or {@code OVERSIZE}
     * @param hint safe context such as a filename or rule description, never secret content
     * @param secretLength the matched length in characters, or the diff size for oversize findings
     * @param fingerprint the first 8 hex characters of the SHA-256 of the match, for log correlation
     */
    public record Finding(String ruleId, String hint, int secretLength, String fingerprint) {
    }

    /**
     * Scans the given diff for blocking findings.
     *
     * @param diff the staged diff to scan, must not be {@code null}
     * @return an unmodifiable list of findings, empty when the diff is safe to send
     * @throws NullPointerException when {@code diff} is {@code null}
     */
    public static List<Finding> scan(String diff) {
        Objects.requireNonNull(diff, "diff must not be null");
        List<Finding> findings = new ArrayList<>();
        int byteLength = diff.getBytes(StandardCharsets.UTF_8).length;
        if (byteLength > MAX_DIFF_BYTES) {
            findings.add(new Finding("OVERSIZE", "diff exceeds " + MAX_DIFF_BYTES + " bytes", byteLength,
                    fingerprint(Integer.toString(byteLength))));
            return Collections.unmodifiableList(findings);
        }
        if (diff.indexOf('\u0000') >= 0) {
            findings.add(new Finding("BINARY", "diff contains binary content", byteLength,
                    fingerprint("binary:" + byteLength)));
            return Collections.unmodifiableList(findings);
        }
        checkLockfiles(diff, findings);
        match(PRIVATE_KEY, "PRIVATE_KEY", "private key block", diff, findings);
        match(CERTIFICATE, "CERTIFICATE", "certificate block", diff, findings);
        match(AWS_ACCESS_KEY, "AWS_ACCESS_KEY", "aws access key", diff, findings);
        match(GITHUB_TOKEN, "GITHUB_TOKEN", "github token", diff, findings);
        match(OPENAI_KEY, "API_KEY", "api key", diff, findings);
        match(JWT, "JWT", "jwt", diff, findings);
        match(BEARER_TOKEN, "BEARER_TOKEN", "bearer token", diff, findings);
        match(GENERIC_ASSIGNMENT, "CREDENTIAL_ASSIGNMENT", "credential assignment", diff, findings);
        match(CONNECTION_STRING, "CONNECTION_STRING", "credentialed connection string", diff, findings);
        return Collections.unmodifiableList(findings);
    }

    /**
     * Summarizes findings for user-facing abort messages without secret content.
     *
     * @param findings the findings to summarize, must not be {@code null}
     * @return a single-line summary such as {@code PRIVATE_KEY(len=48, fp=abc12345)}
     * @throws NullPointerException when {@code findings} is {@code null}
     */
    public static String summarize(List<Finding> findings) {
        Objects.requireNonNull(findings, "findings must not be null");
        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < findings.size(); i++) {
            Finding finding = findings.get(i);
            if (i > 0) {
                summary.append("; ");
            }
            summary.append(finding.ruleId()).append("(len=").append(finding.secretLength())
                    .append(", fp=").append(finding.fingerprint()).append(')');
        }
        return summary.toString();
    }

    private static void checkLockfiles(String diff, List<Finding> findings) {
        Matcher matcher = ADDED_FILE.matcher(diff);
        while (matcher.find()) {
            String filename = matcher.group(1).trim();
            for (String suffix : LOCKED_SUFFIXES) {
                if (filename.endsWith(suffix)) {
                    findings.add(new Finding("LOCKFILE", "generated file " + filename, filename.length(),
                            fingerprint("lockfile:" + filename)));
                    break;
                }
            }
        }
    }

    private static void match(Pattern pattern, String ruleId, String hint, String diff,
            List<Finding> findings) {
        Matcher matcher = pattern.matcher(diff);
        while (matcher.find()) {
            String matched = matcher.group();
            findings.add(new Finding(ruleId, hint, matched.length(), fingerprint(matched)));
        }
    }

    private static String fingerprint(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(8);
            for (int i = 0; i < 4; i++) {
                hex.append(Character.forDigit((hash[i] >> 4) & 0xF, 16));
                hex.append(Character.forDigit(hash[i] & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
