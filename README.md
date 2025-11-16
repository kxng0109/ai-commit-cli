# AI Commit CLI

> Generate conventional commit messages using AI. It supports OpenAI, Anthropic Claude, Google Gemini, DeepSeek, Ollama, and any OpenAI-compatible API.

[![Release](https://img.shields.io/github/v/release/kxng0109/ai-commit-cli)](https://github.com/kxng0109/ai-commit-cli/releases)
[![Release](https://github.com/kxng0109/ai-commit-cli/actions/workflows/release.yml/badge.svg)](https://github.com/kxng0109/ai-commit-cli/actions/workflows/release.yml)
[![CI](https://github.com/kxng0109/ai-commit-cli/actions/workflows/ci.yml/badge.svg)](https://github.com/kxng0109/ai-commit-cli/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## Why This Exists

I was tired of writing vague commit messages like "fix stuff" and "updates", even using sentences like "added ..." or "made ...". I wanted AI to analyze my git diffs and generate proper commit messages, so I started searching for solutions.

**What I found:**
- Tools that didn't work or were too complicated to set up
- Solutions that didn't integrate well with IntelliJ IDEA (my main IDE)
- The JetBrains AI Assistant plugin, which was almost perfect

**The JetBrains AI Assistant Problem:**

The plugin works great and integrates seamlessly with IntelliJ. It can generate commit messages using local models (Ollama, LM Studio) or cloud models, and even do wayyy more things. The free tier is generous, and it gives you access to tons of models. Basically Google's full lineup, Anthropic's Claude, OpenAI (even GPT-5.1).

But here's where I hit a wall:
- **No custom API endpoints** - I couldn't use OpenRouter to access multiple providers with one key
- **No DeepSeek** - What if I wanted to use their cost-effective API directly?
- **IDE-locked** - Only works in JetBrains IDEs (requires IntelliJ IDEA Ultimate license)
- **Not portable** - What if I switch to VSCode, Sublime Text, or just use the terminal?

**I wanted freedom:**
- Use models from *anywhere* - OpenRouter, DeepSeek, local Ollama, custom endpoints
- Work with *any* editor/IDE
- Run from the terminal on *any* platform
- Own my workflow completely

So I built this. A standalone CLI that works everywhere, supports any OpenAI-compatible API, you determine the cost (you can use free or paid providers, the choice is yours), and never locks you into a specific IDE or subscription.

## Features

- **True Multi-Provider Support** - OpenAI, Anthropic, Google Gemini, DeepSeek, Ollama, or any OpenAI-compatible API (OpenRouter, Together AI, etc.)
- **Use Any API** - Not limited to specific providers. Bring your own endpoint
- **Conventional Commits** - Follows [specification](https://www.conventionalcommits.org/) automatically
- **Native Binary** - No runtime dependencies, starts in <50ms
- **Cross-Platform** - Linux, macOS, Windows
- **Privacy Option** - Use Ollama for 100% local processing (but be sure that you're using a model that your system can handle)
- **IDE-Agnostic** - Works from any terminal, any editor
- **Cost Control** - Use free providers or choose your own pricing tier

## Installation

### Option 1: One-Line Installer (Easiest)

**Linux/macOS:**
```bash
curl -fsSL https://raw.githubusercontent.com/kxng0109/ai-commit-cli/main/install.sh | bash
```

**Windows (PowerShell as Administrator):**
```powershell
irm https://raw.githubusercontent.com/kxng0109/ai-commit-cli/main/install.ps1 | iex
```

### Option 2: Download Binary Manually

Download the pre-built binary for your platform from [Releases](https://github.com/kxng0109/ai-commit-cli/releases/latest).

**Linux:**
```bash
curl -L https://github.com/kxng0109/ai-commit-cli/releases/latest/download/ai-commit-linux-amd64 -o ai-commit
chmod +x ai-commit
sudo mv ai-commit /usr/local/bin/
ai-commit --version
```

**macOS:**
```bash
curl -L https://github.com/kxng0109/ai-commit-cli/releases/latest/download/ai-commit-macos-amd64 -o ai-commit
chmod +x ai-commit
sudo mv ai-commit /usr/local/bin/
ai-commit --version
```

**Windows (PowerShell):**
```powershell
Invoke-WebRequest -Uri "https://github.com/kxng0109/ai-commit-cli/releases/latest/download/ai-commit-windows-amd64.exe" -OutFile "ai-commit.exe"
# Move to a directory in your PATH, e.g., C:\
Move-Item ai-commit.exe C:\
ai-commit --version
```

### Build from Source

**Requirements:**
- GraalVM 25 (with native-image)
- Apache Maven 3.9.11
- Java 25

**Steps:**
```bash
git clone https://github.com/kxng0109/ai-commit-cli.git
cd ai-commit-cli

mvn clean package -Pnative

# Binary location: target/ai-commit (or ai-commit.exe on Windows)
sudo cp target/ai-commit /usr/local/bin/  # Linux/macOS
# or add to PATH on Windows

ai-commit --version
```

**Development build (JAR, faster):**
```bash
mvn clean package
java -jar target/ai-commit-cli-1.0.0.jar --version
```

## Quick Start

**1. Configure AI Provider** (choose one):

```bash
# OpenAI
export OPENAI_API_KEY="sk-..."

# Anthropic Claude
export ANTHROPIC_API_KEY="sk-ant-..."

# Google Gemini
export GOOGLE_API_KEY="AIza..."

# DeepSeek
export DEEPSEEK_API_KEY="..."

# Ollama (local)
export OLLAMA_MODEL="llama3.2"
```

**2. Use:**

```bash
cd your-git-repo
git add .
ai-commit
```

## Configuration

### AI Providers

| Provider | Required | Optional |
|----------|----------|----------|
| **OpenAI** | `OPENAI_API_KEY` | `OPENAI_MODEL` (default: gpt-4o-mini)<br>`OPENAI_BASE_URL` (default: https://api.openai.com) |
| **Anthropic** | `ANTHROPIC_API_KEY` | `ANTHROPIC_MODEL` (default: claude-sonnet-4-0) |
| **Google** | `GOOGLE_API_KEY` | `GOOGLE_MODEL` (default: gemini-2.0-flash) |
| **DeepSeek** | `DEEPSEEK_API_KEY` | - |
| **Ollama** | `OLLAMA_MODEL` | `OLLAMA_BASE_URL` (default: http://localhost:11434) |

**Priority:** OpenAI → Anthropic → Google → DeepSeek → Ollama

### Optional Settings

| Variable | Default | Range |
|----------|---------|-------|
| `AI_TEMPERATURE` | 0.1 | 0.0 - 2.0 |
| `AI_COMMAND_TIMEOUT` | 30 | 1 - 3600 seconds |
| `AI_LOG_LEVEL` | WARN | ERROR, WARN, INFO, DEBUG |

## Examples

**OpenAI:**
```bash
export OPENAI_API_KEY="sk-..."
git add .
ai-commit
```

**OpenRouter (multiple providers):**
```bash
export OPENAI_API_KEY="sk-or-..."
export OPENAI_BASE_URL="https://openrouter.ai/api/v1"
export OPENAI_MODEL="openai/gpt-oss-20b:free"
git add .
ai-commit
```

**Ollama (100% local):**
```bash
ollama serve &
export OLLAMA_MODEL="llama3.2"
git add .
ai-commit
```

## How It Works

1. **Stage your changes:** `git add .`
2. **Run ai-commit:** Analyzes the git diff
3. **AI generates message:** Sends to your configured provider (OpenAI, OpenRouter, Ollama, etc.)
4. **Auto-commits:** Applies the formatted conventional commit message

```
Your staged diff → AI analysis → feat(auth): add OAuth2 flow → committed
```

## Real-World Comparison

**JetBrains AI Assistant (what I was using):**
```
✅ Great IDE integration
✅ Generous free tier
✅ Many models (Google, Anthropic, OpenAI)
❌ Can't use OpenRouter/DeepSeek/custom APIs
❌ Requires IntelliJ IDEA Ultimate
❌ IDE-locked (no terminal/other editors)
```

**AI Commit CLI (what I built):**
```
✅ Use ANY provider (OpenRouter, DeepSeek, custom endpoints)
✅ Works anywhere (any IDE, terminal, SSH sessions)
✅ Free and open source
✅ Full control over API costs
✅ 100% local option with Ollama
✅ ~99MB binary, no dependencies
```

## Example Output

**Before:**
```bash
git commit -m "updated auth stuff"
```

**After:**
```bash
$ ai-commit

AI generated commit message:
────────────────────────────────────────────────────────────
feat(auth): add OAuth2 authentication flow

Implement Google OAuth2 integration with JWT token handling,
refresh token rotation, and secure session management.

BREAKING CHANGE: API now requires OAuth2 tokens
────────────────────────────────────────────────────────────

[main abc1234] feat(auth): add OAuth2 authentication flow
 3 files changed, 145 insertions(+), 12 deletions(-)
```

## Advanced Configuration

### Using OpenRouter (Multiple Models via One API)

Access Claude, GPT-4, Llama, and more through [OpenRouter](https://openrouter.ai):

```bash
export OPENAI_API_KEY="sk-or-v1-..."
export OPENAI_BASE_URL="https://openrouter.ai/api/v1"
export OPENAI_MODEL="qwen/qwen3-coder"
# or: "openai/gpt-4o", "meta-llama/llama-3-70b", etc.
git add .
ai-commit
```

### Temperature Control

Higher = more creative, lower = more deterministic:

```bash
export AI_TEMPERATURE="0.0"  # Very consistent
export AI_TEMPERATURE="0.5"  # Balanced (recommended for variety)
export AI_TEMPERATURE="1.5"  # More creative descriptions
```

### Debugging

```bash
export AI_LOG_LEVEL="DEBUG"
ai-commit
```

### Long-running Git Operations

```bash
export AI_COMMAND_TIMEOUT="120"  # 2 minutes (default: 30)
```

## Technical Details

**Built With:**
- Java 25
- Apache Maven 3.9.11
- GraalVM 25 (native-image)
- Spring AI 1.1.0

**Binary Size:** ~99MB (includes all 5 AI providers)

**Startup Time:** <50ms (native binary)

**Supported Platforms:**
- Linux x86_64 (glibc 2.17+)
- macOS x86_64 (10.13+)
- Windows x86_64 (10+)

## Troubleshooting

**"No staged changes found"**
```bash
git status  # Verify you're in a git repo
git add .   # Stage your changes first
```

**"Cannot connect to AI provider"**
```bash
# Verify API key is set
echo $OPENAI_API_KEY

# Test with debug logging
export AI_LOG_LEVEL=DEBUG
ai-commit
```

**"Command not found"**
- Ensure binary is in PATH
- Restart terminal after installation

## Development

```bash
# Build JAR (fast iteration)
mvn clean package
java -jar target/ai-commit-cli-1.0.0.jar

# Build native binary
mvn clean package -Pnative

# Run with debug logging
export AI_LOG_LEVEL=DEBUG
./target/ai-commit
```

## FAQ

**Q: Why not just use JetBrains AI Assistant?**  
A: It's excellent for IntelliJ users, but it limits you to their provider selection (no OpenRouter, DeepSeek, or custom APIs), requires IntelliJ IDEA Ultimate, and only works inside JetBrains IDEs. This tool works everywhere and with any provider.

**Q: Which AI provider should I use?**  
A: Honestly, it's your choice. For instance, I use OpenRouter mainly since you can get really good models for free and have access to a lot of models with just one API key. Then I use ollama (or LM Studio) just for testing. You can get professional grade models, but you're limited by your system's hardware.

**Q: Does this send my code to AI providers?**  
A: It sends the git diff (changes only), not your entire codebase. Use Ollama for fully local processing with zero external API calls.

**Q: Can I edit the message before committing?**  
A: Not yet - it auto-commits immediately. Use `git commit --amend` to edit afterward. Interactive mode is planned for v1.1.0.

**Q: How much does it cost?**  
A: Depends on your provider:
- OpenAI gpt-4o-mini: ~$0.0001-0.001 per commit
- DeepSeek: ~$0.00001 per commit
- Ollama: $0 (free, local)
- OpenRouter: Varies by model

**Q: Why is the binary 99MB?**  
A: It includes 5 AI provider SDKs and the GraalVM runtime for instant startup. Future versions may offer slim builds with only the providers you need.

**Q: Does it work with monorepos?**  
A: Yes, it analyzes all staged changes regardless of repo structure.

**Q: Can I use this in CI/CD?**  
A: Yes! Set environment variables and run in automated workflows. Great for generating commit messages from automated tools.

## Contributing

Contributions welcome! See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## License

[MIT](LICENSE) - use freely, commercially or personally.

## Acknowledgments

- Built with [Spring AI](https://spring.io/projects/spring-ai)
- Compiled with [GraalVM](https://www.graalvm.org/)
- Inspired by [Conventional Commits](https://www.conventionalcommits.org/)

## Support

- [Documentation](https://github.com/kxng0109/ai-commit-cli)
- [Report Issues](https://github.com/kxng0109/ai-commit-cli/issues)
- [Request Features](https://github.com/kxng0109/ai-commit-cli/issues/new?labels=enhancement)

## Related Projects

- [JetBrains AI Assistant](https://www.jetbrains.com/ai/) - IDE-integrated AI (IntelliJ IDEA Ultimate)
- [OpenRouter](https://openrouter.ai/) - Unified API for 200+ AI models
- [Ollama](https://ollama.ai/) - Run AI models locally

---

**Built because I wanted to use OpenRouter and DeepSeek from anywhere, not just IntelliJ. If it helps you too, give it a star 🤗**
