# Quick Start

## 1. Install

**One-line install:**

```bash
# Linux/macOS
curl -fsSL https://raw.githubusercontent.com/kxng0109/ai-commit-cli/main/install.sh | bash

# Windows (PowerShell)
irm https://raw.githubusercontent.com/kxng0109/ai-commit-cli/main/install.ps1 | iex
```

**Or download binary:**
- [Linux](https://github.com/kxng0109/ai-commit-cli/releases/latest/download/ai-commit-linux-amd64)
- [macOS](https://github.com/kxng0109/ai-commit-cli/releases/latest/download/ai-commit-macos-amd64)
- [Windows](https://github.com/kxng0109/ai-commit-cli/releases/latest/download/ai-commit-windows-amd64.exe)

## 2. Configure

**Pick ONE AI provider:**

```bash
# OpenAI
export OPENAI_API_KEY="sk-..."

# Or Anthropic
export ANTHROPIC_API_KEY="sk-ant-..."

# Or Google Gemini
export GOOGLE_API_KEY="AIza..."

# Or DeepSeek
export DEEPSEEK_API_KEY="..."

# Or Ollama (local, free)
export OLLAMA_MODEL="llama3"
```

**Make it permanent (But be careful. API keys are meant to be private):**

```bash
# Linux/macOS
echo 'export OPENAI_API_KEY="sk-..."' >> ~/.bashrc
source ~/.bashrc

# Windows
setx OPENAI_API_KEY "sk-..."
```

## 3. Use

```bash
cd your-project
git add .
ai-commit
```

**That's it!** Your changes are committed with an AI-generated message.

## Example Output

```
AI generated commit message:
────────────────────────────────────────────────────────────
feat(auth): add OAuth2 authentication flow

Implement Google OAuth2 integration with JWT token handling
and secure session management.
────────────────────────────────────────────────────────────

[main abc1234] feat(auth): add OAuth2 authentication flow
 3 files changed, 145 insertions(+)
```

## Common Use Cases

**OpenRouter (access multiple models):**
```bash
export OPENAI_API_KEY="sk-or-..."
export OPENAI_BASE_URL="https://openrouter.ai/api/v1"
export OPENAI_MODEL="anthropic/claude-3.5-sonnet"
```

**Higher creativity:**
```bash
export AI_TEMPERATURE="0.7"
```

**Debug issues:**
```bash
export AI_LOG_LEVEL="DEBUG"
ai-commit
```

## Troubleshooting

**"Command not found"**
- Restart terminal after install
- Check: `which ai-commit` (Unix) or `where ai-commit` (Windows)

**"No staged changes"**
- Run: `git add .` first
- Check: `git status`

**"Cannot connect"**
- Verify API key: `echo $OPENAI_API_KEY`
- Check internet connection

## Next Steps

- See [README.md](README.md) for full documentation
- Get API keys:
    - [OpenAI](https://platform.openai.com/api-keys)
    - [Anthropic](https://console.anthropic.com/)
    - [Google](https://aistudio.google.com/app/api-keys)
    - [DeepSeek](https://platform.deepseek.com/)