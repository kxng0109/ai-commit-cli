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

## Usage Modes

### Interactive Mode (Default)
```bash
git add .
ai-commit
# Review → Choose: (y)es / (r)egenerate / (e)dit / (c)ancel
```

### Auto-Commit Mode (Fast!)
```bash
# One-time setup
ai-commit config --auto-commit on

# Daily usage (no prompts!)
git add .
ai-commit  # Commits instantly
```

### Auto-Push Mode (Works with both!)
```bash
# Option 1: Review commits + auto-push (recommended)
ai-commit config --auto-commit off
ai-commit config --auto-push on

# Daily usage
git add .
ai-commit  # Shows prompts → Accept → Auto-pushes

# Option 2: Full automation
ai-commit config --auto-commit on
ai-commit config --auto-push on

# Daily usage (commits and pushes)
git add .
ai-commit  # Done!
```

## Configuration Commands

```bash
# View settings
ai-commit config --show

# Enable auto-commit (skip prompts)
ai-commit config --auto-commit on

# Enable auto-push (push after commit)
ai-commit config --auto-push on

# Disable automation
ai-commit config --auto-commit off
ai-commit config --auto-push off

# Reset everything
ai-commit config --reset
```

**Settings persist forever** (even after restart)

## Example Output

**Interactive Mode:**
```
AI generated commit message:
────────────────────────────────────────────────────────────
feat(auth): add OAuth2 authentication flow

Implement Google OAuth2 integration with JWT token handling
and secure session management.
────────────────────────────────────────────────────────────

Commit with this message? (y)es / (r)egenerate / (e)dit / (c)ancel [y]: 

[main abc1234] feat(auth): add OAuth2 authentication flow
 3 files changed, 145 insertions(+)
```

**Auto-Commit Mode:**
```
AI generated commit message:
────────────────────────────────────────────────────────────
feat(auth): add OAuth2 authentication flow

Implement Google OAuth2 integration with JWT token handling
and secure session management.
────────────────────────────────────────────────────────────

Auto-committing...

[main abc1234] feat(auth): add OAuth2 authentication flow
 3 files changed, 145 insertions(+)

Auto-pushing...

To github.com:user/repo.git
   def5678..abc1234  main -> main
```

## Common Use Cases

**OpenRouter (access multiple models):**
```bash
export OPENAI_API_KEY="sk-or-..."
export OPENAI_BASE_URL="https://openrouter.ai/api/v1"
export OPENAI_MODEL="anthropic/claude-3.5-sonnet"
```

**Rapid development workflow:**
```bash
# One-time setup
ai-commit config --auto-commit on
ai-commit config --auto-push on

# Daily usage
git add feature.js
ai-commit  # Committed and pushed instantly
```

**Review commits, auto-push (recommended!):**
```bash
# One-time setup
ai-commit config --auto-commit off
ai-commit config --auto-push on

# Daily usage
git add feature.js
ai-commit
# Review message → Accept → Auto-pushes
```

**Careful review workflow:**
```bash
# Disable auto-commit for important changes
ai-commit config --auto-commit off
ai-commit config --auto-push off

git add important-feature.js
ai-commit
# Review carefully, regenerate if needed, then accept
git push  # Manual push
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

**"Auto-commit not working"**
- Check: `ai-commit config --show`
- Enable: `ai-commit config --auto-commit on`

**"Auto-push requires auto-commit"**
- Check: `ai-commit config --show`
- Note: Auto-push now works independently. It pushes after accepting commits in interactive mode as well.

## Next Steps

- See [README.md](README.md) for full documentation
- Get API keys:
  - [OpenAI](https://platform.openai.com/api-keys)
  - [Anthropic](https://console.anthropic.com/)
  - [Google](https://aistudio.google.com/app/api-keys)
  - [DeepSeek](https://platform.deepseek.com/)
- Explore [OpenRouter](https://openrouter.ai) for access to 200+ models