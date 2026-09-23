# ai-commit bash completion. Install: ai-commit completion bash >> ~/.bash_completion
# Regenerate this file when flags change: keep in sync with AiCommitCli --help.
_ai_commit() {
    local cur prev words cword
    _init_completion || return
    local flags="--yes -y --dry-run --amend -a --all --model --provider --version -v --help -h"
    local commands="config completion"
    local config_flags="--show --auto-commit --auto-push --reset --help"
    local providers="openai anthropic google deepseek ollama"
    local shells="bash zsh fish powershell"

    case "$prev" in
        --model)
            return 0
            ;;
        --provider)
            COMPREPLY=($(compgen -W "$providers" -- "$cur"))
            return 0
            ;;
        completion)
            COMPREPLY=($(compgen -W "$shells" -- "$cur"))
            return 0
            ;;
        config)
            COMPREPLY=($(compgen -W "$config_flags" -- "$cur"))
            return 0
            ;;
        --auto-commit|--auto-push)
            COMPREPLY=($(compgen -W "on off" -- "$cur"))
            return 0
            ;;
    esac

    if [[ "$cur" == -* ]]; then
        COMPREPLY=($(compgen -W "$flags" -- "$cur"))
        return 0
    fi
    COMPREPLY=($(compgen -W "$commands" -- "$cur"))
}
complete -F _ai_commit ai-commit
