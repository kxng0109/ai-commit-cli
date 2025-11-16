# Contributing to AI Commit CLI

## Development Setup

**Prerequisites:**
- GraalVM 25 or Java 25
- Maven 3.9+
- Git

**Clone and build:**
```bash
git clone https://github.com/kxng0109/ai-commit-cli.git
cd ai-commit-cli
mvn clean package
```

## Making Changes

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Make changes and test locally
4. Commit using conventional commits: `feat: add feature`
5. Push and create a Pull Request

## Commit Convention

Use [Conventional Commits](https://www.conventionalcommits.org/):

- `feat:` - New feature
- `fix:` - Bug fix
- `docs:` - Documentation
- `refactor:` - Code refactoring
- `test:` - Test additions
- `chore:` - Build/tooling changes

Examples:
```bash
feat(api): add support for new AI provider
fix(git): resolve timeout on large repos
docs(readme): update installation instructions
```

## Testing

```bash
# Build and test JAR
mvn clean package
java -jar target/ai-commit-cli-1.0.0.jar --version

# Test native build
mvn clean package -Pnative
./target/ai-commit --version

# Test in real repo
cd /tmp
git init test-repo && cd test-repo
echo "test" > file.txt
git add .
/path/to/ai-commit
```

## Pull Request Guidelines

- Keep PRs focused on single changes
- Update documentation if needed
- Ensure CI passes on all platforms
- Add description explaining changes

## Code Style

- Follow existing code patterns
- Use meaningful variable names
- Add JavaDoc for public methods
- Keep methods focused and concise

## Questions?

Open an [issue](https://github.com/kxng0109/ai-commit-cli/issues) or discussion.