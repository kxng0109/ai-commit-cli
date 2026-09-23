# ai-commit PowerShell completion. Install: ai-commit completion powershell >> $PROFILE
# Regenerate this file when flags change: keep in sync with AiCommitCli --help.
Register-ArgumentCompleter -Native -CommandName ai-commit -ScriptBlock {
    param($wordToComplete, $commandAst, $cursorPosition)
    $flags = @('--yes', '-y', '--dry-run', '--amend', '-a', '--all', '--model', '--provider', '--version', '-v', '--help', '-h')
    $commands = @('config', 'completion')
    $elements = $commandAst.CommandElements | Select-Object -Skip 1 | ForEach-Object { "$_" }
    $prev = if ($elements.Count -ge 1) { $elements[$elements.Count - 1] } else { '' }
    $candidates = @()
    switch ($prev) {
        '--provider' { $candidates = @('openai', 'anthropic', 'google', 'deepseek', 'ollama') }
        'completion' { $candidates = @('bash', 'zsh', 'fish', 'powershell') }
        'config' { $candidates = @('--show', '--auto-commit', '--auto-push', '--reset', '--help') }
        '--auto-commit' { $candidates = @('on', 'off') }
        '--auto-push' { $candidates = @('on', 'off') }
        default {
            $candidates = $flags
            if ($elements.Count -eq 0 -or ($elements.Count -eq 1 -and $wordToComplete -ne '' -and -not $wordToComplete.StartsWith('-'))) {
                $candidates += $commands
            }
        }
    }
    $candidates | Where-Object { $_ -like "$wordToComplete*" } | ForEach-Object {
        [System.Management.Automation.CompletionResult]::new($_, $_, 'ParameterName', $_)
    }
}
