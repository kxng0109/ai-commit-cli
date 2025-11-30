# Test Suite Summary

## Total Tests: 56

---

### ConfigTest.java (9 tests)
- Validates config loading logic from environment with all key edge cases (default values, invalid/null/blank settings).
- Precisely checks that each provider (OpenAI, Anthropic, Google Gemini, DeepSeek, Ollama) is only activated when correctly configured, preventing silent misconfiguration for all APIs.
- Ensures that model name and API key requirements are strictly enforced to avoid errors at runtime.

**Coverage:** ~100% of all provider config logic, validation, and public guards.

---

### UserPreferencesTest.java (10 tests)
- Covers all permutations of enabling/disabling auto-commit and auto-push, including persistence and correct isolation between tests.
- Tests reset logic and verifies formatted display accurately represents user current settings for CLI status.
- Assures changes to preferences are reliably saved, displayed, and reset across runs and test executions.

**Coverage:** ~100% of static API, preference logic, and state management.

---

### AiProviderFactoryTest.java (4 tests)
- Validates correct AI provider prioritization (OpenAI > Anthropic > Google > DeepSeek > Ollama), and ensures error thrown if no provider is configured.
- Checks that bad usage (utility class constructor) fails immediately, avoiding accidental misuse.
- Confirms provider selection logic always returns a valid and supported provider instance when configuration is correct.

**Coverage:** ~100% of factory provider logic, priority, and error handling.

---

### GitServiceTest.java (6 tests)
- Performs real git command cycles to check repo detection, staged change identification, and commit command success/failure for both valid and invalid directories.
- Tests working directory handling robustly, including detection outside git repos and correct behavior regardless of user environment.
- Verifies staged diff generation and commit logic, confirming platform compatibility and integration.

**Coverage:** ~90% of public GitService API, including common and edge-case scenarios.

---

### CommitServiceTest.java (15 tests)
- Simulates user-driven commit flows under all possible interactive choices (accept, cancel, regenerate, edit, invalid, retry), carefully handling errors, whitespace trimming, incorrect/empty AI responses.
- Exercises AI integration and message preparation for all relevant paths, ensuring commit proceeds only under valid conditions.
- Provides deep testing of workflow, error handling, message formatting, and decision logic for a robust interactive commit experience.

**Coverage:** ~95% of core behavior and input/output/logical branches.

---

### CommitServiceAutoPushTest.java (12 tests)
- Exhaustively tests auto-commit and auto-push combinations, verifying push and commit only occur as expected, with correct handling of push failures, multiple regenerations, and workflow idempotency.
- Validates safe operation under user input, interaction, and error-prone scenarios—never pushes or commits unless all correct conditions are met.
- Confirms double-pushes, unwanted commits, and failures are avoided in all tested branches.

**Coverage:** ~100% of auto-commit/push workflow logic, including error and edge handling.

---

## Running Tests

```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=ConfigTest

# Skip tests during build
mvn package -DskipTests
```

## Test Dependencies

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>6.0.1</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <version>5.20.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <version>5.20.0</version>
    <scope>test</scope>
</dependency>
```

## Test Coverage Table

| Class                     | Tests | Coverage                                    |
|---------------------------|-------|---------------------------------------------|
| ConfigTest                | 9     | ~100% provider/config logic and guards      |
| UserPreferencesTest       | 10    | ~100% preference/state/formatting           |
| AiProviderFactoryTest     | 4     | ~100% provider selection/prioritization     |
| GitServiceTest            | 6     | ~90% public GitService usage scenarios      |
| CommitServiceTest         | 15    | ~95% workflow, interactive, AI             |
| CommitServiceAutoPushTest | 12    | ~100% all auto-commit/push paths            |

---

## CI Integration

Tests run automatically on:
- Every push to `main`/`develop`
- Every pull request
- Before releases (optional in workflow)

See `.github/workflows/ci.yml`

---

## Notes

- **GitServiceTest** requires `git` installed and in PATH
- **CommitServiceTest** and **CommitServiceAutoPushTest** use mocks (no real AI or git network calls)
- **AiProviderFactoryTest** instantiates provider objects without making API calls
- **UserPreferencesTest** uses the Java Preferences API and resets between each test
- All tests complete quickly (<5 seconds), with clean state
- Tear-down ensures no test pollution between runs
