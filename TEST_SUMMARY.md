# Test Suite Summary

## Total Tests: 27 + New Tests for Configuration Features

### ConfigTest.java (10 tests)
**What it tests:** Configuration loading and validation

- Default values loading  
- OpenAI config validation (null/blank/valid)  
- Anthropic config validation  
- Google config validation  
- DeepSeek config validation  
- Ollama config validation

**Coverage:**
- Environment variable parsing
- Default value handling
- API key validation
- Model configuration

---

### AiProviderFactoryTest.java (4 tests)
**What it tests:** AI provider creation and priority

- Throws exception when no provider configured  
- Creates ChatModel with OpenAI  
- Prioritizes OpenAI over other providers  
- Constructor throws UnsupportedOperationException (utility class)

**Coverage:**
- Provider initialization
- Priority ordering (OpenAI > Anthropic > Google > DeepSeek > Ollama)
- Error handling for missing config

---

### GitServiceTest.java (6 tests)
**What it tests:** Git command execution (integration tests)

- Service creation with timeout  
- Returns false for non-git directory  
- Returns false when no changes staged  
- Returns true when files are staged  
- Gets staged diff content  
- Commits with valid message

**Coverage:**
- Git repository detection
- Staged changes detection
- Diff generation
- Commit execution
- Working directory handling

**Note:** Requires `git` command in PATH

---

### CommitServiceTest.java (7 tests)
**What it tests:** Commit message generation logic (mocked)

- Service creation  
- Throws exception when no staged changes  
- Successfully generates and commits  
- Handles empty AI response  
- Handles null AI response  
- Handles whitespace-only response  
- Handles AI API exceptions  
- Trims whitespace from AI responses

**Coverage:**
- Pre-commit validation
- AI integration (mocked)
- Error handling
- Message formatting

---

## Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
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

## Test Coverage

| Class | Tests   | Coverage |
|-------|---------|----------|
| Config | 10      | ~90% (all public methods) |
| AiProviderFactory | 4       | ~70% (main logic + error handling) |
| GitService | 6       | ~80% (all public methods, requires git) |
| CommitService | 7       | ~85% (all scenarios + edge cases) |
| UserPreferences | 12      | Persistent storage operations |

## CI Integration

Tests run automatically on:
- Every push to main/develop
- Every pull request
- Before releases (optional in workflow)

See `.github/workflows/ci.yml`

## Notes

- **GitServiceTest** requires `git` installed and in PATH
- **CommitServiceTest** uses mocks (no real AI calls)
- **AiProviderFactoryTest** creates real provider instances (no API calls)
- **UserPreferencesTest** tests Java Preferences API (isolated per test)
- All tests are fast (<5 seconds total)
- Cleanup methods ensure no test pollution between runs