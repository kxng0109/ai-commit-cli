# ai-commit fish completion. Install: ai-commit completion fish > ~/.config/fish/completions/ai-commit.fish
# Regenerate this file when flags change: keep in sync with AiCommitCli --help.
complete -c ai-commit -f
complete -c ai-commit -s y -l yes -d 'Commit without prompting'
complete -c ai-commit -l dry-run -d 'Print the message without committing'
complete -c ai-commit -l amend -d 'Amend the previous commit'
complete -c ai-commit -s a -l all -d 'Stage tracked modifications'
complete -c ai-commit -l model -d 'Use this model for the run' -r
complete -c ai-commit -l provider -d 'Use this provider for the run' -r -a 'openai anthropic google deepseek ollama'
complete -c ai-commit -s v -l version -d 'Print version'
complete -c ai-commit -s h -l help -d 'Print help'
complete -c ai-commit -n __fish_use_subcommand -a config -d 'Manage configuration'
complete -c ai-commit -n __fish_use_subcommand -a completion -d 'Print shell completions'
complete -c ai-commit -n '__fish_seen_subcommand_from config' -a '--show --auto-commit --auto-push --reset --help'
complete -c ai-commit -n '__fish_seen_subcommand_from completion' -a 'bash zsh fish powershell'
