# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.2.0] - 2025-11-29

### Added
- **Persistent user preferences** - Settings now persist across application restarts
  - `ai-commit config --auto-commit [on|off]` - Enable/disable automatic commit without prompts
  - `ai-commit config --auto-push [on|off]` - Enable/disable automatic push after committing
  - `ai-commit config --show` - Display current configuration settings
  - `ai-commit config --reset` - Reset all settings to defaults
  - Settings stored using Java Preferences API (Registry on Windows, .plist on macOS, XML on Linux)
- **Auto-commit mode** - Skip interactive prompts and commit immediately after AI generation
  - Significantly faster workflow for rapid development
  - Automatically aborts if AI generation fails (no changes committed)
  - Warning displayed when enabling about loss of regenerate/edit/cancel options
- **Auto-push mode** - Automatically push changes after committing
  - Works independently with both interactive and auto-commit modes
  - With auto-commit OFF: Pushes after accepting in interactive prompt (review commits, skip manual push)
  - With auto-commit ON: Pushes immediately after auto-commit (full automation)
  - Gracefully handles push failures (commit still succeeds)
- **Configuration command system** - New `config` subcommand for managing preferences
  - `ai-commit config --help` - Show configuration help
  - Interactive and auto modes coexist - easily switch between workflows

### Changed
- `CommitService` now checks user preferences before deciding workflow mode
- Interactive prompts only shown when auto-commit is disabled
- Help text updated to include configuration commands
- README and documentation updated with auto-commit/auto-push examples

### Technical
- Added `UserPreferences` class for persistent storage management
- Updated `CommitService` with auto-commit and auto-push logic
- Added `push()` method to `GitService` for git push operations
- Enhanced error handling for auto-commit mode

## [1.1.1] - 2025-11-21

### Fixed
- **Working directory detection** - Fixed issue where CLI tool couldn't detect staged changes when run from compiled binary
  - Tool now correctly detects the actual current working directory instead of using the binary's location
  - Improved cross-platform working directory resolution with `PWD` environment variable support on Unix-like systems
  - Universal fallback using absolute path resolution works reliably on Windows, Linux, and macOS
  - Resolves "No staged changes found" error when staged files were actually present
- **Windows process hanging** - Fixed timeout issue on Windows when running git commands with large diff output
  - Implemented asynchronous stream reading to prevent buffer overflow and process deadlocks
  - Git commands now read stdout and stderr concurrently in separate threads
  - Added `GIT_PAGER=cat` environment variable to disable interactive pager prompts
- **Logging configuration** - Fixed `AI_LOG_LEVEL` environment variable not being respected
  - Moved logging configuration to static block to execute before logger initialization
  - Debug logging now works correctly when `AI_LOG_LEVEL=DEBUG` is set

### Changed
- Enhanced `GitService` with more robust process handling for Windows compatibility
- Improved debug logging throughout Git operations for better troubleshooting

### Technical
- Refactored `GitService.runCommand()` to use concurrent stream reading
- Updated working directory detection to use `PWD` (Unix) with universal fallback
- Fixed SLF4J Simple Logger initialization timing issue

## [1.1.0] - 2025-11-18

### Added
- **Interactive commit confirmation** - Users can now review AI-generated messages before committing
  - `(y)es` - Accept and commit with the generated message
  - `(r)egenerate` - Generate a new message with AI
  - `(e)dit` - Manually edit the message before committing
  - `(c)ancel` - Cancel the commit operation
- Default action (Enter) accepts the commit for faster workflow

### Changed
- Commit workflow now includes user confirmation step
- Improved user experience with clearer prompts

## [1.0.0] - 2025-11-16

### Added
- Initial release
- Support for OpenAI, Anthropic Claude, Google Gemini, DeepSeek, and Ollama
- Conventional Commits format generation
- Cross-platform native binaries (Linux, macOS, Windows)
- Configurable temperature (0.0-2.0)
- Configurable command timeout (1-3600 seconds)
- Debug logging support (ERROR, WARN, INFO, DEBUG)
- OpenRouter and OpenAI-compatible API support

### Technical
- Built with Java 25
- Apache Maven 3.9.11
- GraalVM 25 native compilation
- Spring AI 1.1.0
- Native binary size: ~99MB

[Unreleased]: https://github.com/kxng0109/ai-commit-cli/compare/v1.2.0...HEAD
[1.2.0]: https://github.com/kxng0109/ai-commit-cli/releases/tag/v1.2.0
[1.1.1]: https://github.com/kxng0109/ai-commit-cli/releases/tag/v1.1.1
[1.1.0]: https://github.com/kxng0109/ai-commit-cli/releases/tag/v1.1.0
[1.0.0]: https://github.com/kxng0109/ai-commit-cli/releases/tag/v1.0.0