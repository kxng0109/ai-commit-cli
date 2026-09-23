#compdef ai-commit
# ai-commit zsh completion. Install: ai-commit completion zsh > ~/.zsh/completions/_ai-commit
# Regenerate this file when flags change: keep in sync with AiCommitCli --help.
_ai-commit() {
    local context state line
    typeset -A opt_args
    local flags=(
        '(-y --yes)'{-y,--yes}'[commit without prompting]'
        '--dry-run[print the message without committing]'
        '--amend[amend the previous commit]'
        '(-a --all)'{-a,--all}'[stage tracked modifications]'
        '--model+[use this model for the run]:model:'
        '--provider+[use this provider for the run]:provider:(openai anthropic google deepseek ollama)'
        '(-v --version)'{-v,--version}'[print version]'
        '(-h --help)'{-h,--help}'[print help]'
    )
    _arguments -C $flags \
        '1: :->command' \
        '*:: :->args' && return 0
    case $state in
        command)
            _describe 'command' '(config completion)' && return 0
            ;;
        args)
            case $line[1] in
                config)
                    _values 'config option' --show --auto-commit --auto-push --reset --help && return 0
                    ;;
                completion)
                    _values 'shell' bash zsh fish powershell && return 0
                    ;;
            esac
            ;;
    esac
    return 1
}
compdef _ai-commit ai-commit
