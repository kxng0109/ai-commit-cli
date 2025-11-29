package io.github.kxng0109.config;

import java.util.prefs.Preferences;

/**
 * The {@code UserPreferences} class provides functionality for managing user preferences
 * related to version control operations such as auto-committing and auto-pushing.
 *
 * <p>This utility class stores and retrieves preferences for:
 * <ul>
 *   <li><strong>Auto-commit:</strong> Automatically committing changes, which can bypass
 *       manual intervention and certain actions like message editing or commit cancellation.</li>
 *   <li><strong>Auto-push:</strong> Automatically pushing changes after they are committed.</li>
 * </ul>
 * These preferences are persisted using the {@link Preferences} API.
 *
 * <p>Key features:
 * <ul>
 *   <li>Retrieve the current auto-commit and auto-push settings ({@link #isAutoCommitEnabled()},
 *       {@link #isAutoPushEnabled()}).</li>
 *   <li>Enable or disable auto-commit and auto-push ({@link #setAutoCommit(boolean)},
 *       {@link #setAutoPush(boolean)}).</li>
 *   <li>Reset all preferences to their default state ({@link #reset()}).</li>
 *   <li>Display the current settings in a readable format ({@link #displaySettings()}).</li>
 * </ul>
 *
 * <p><strong>Note:</strong> If auto-commit is enabled, certain manual operations like editing
 * commit messages or canceling commits may not be possible.
 */
public class UserPreferences {

    private static final Preferences preferences = Preferences.userNodeForPackage(UserPreferences.class);

    private static final String AUTO_COMMIT_KEY = "auto_commit";
    private static final String AUTO_PUSH_KEY = "auto_push";

    /**
     * Checks whether the auto-commit feature is enabled.
     *
     * <p>Auto-commit, when enabled, automates the process of committing changes. This setting
     * can bypass manual intervention such as editing commit messages or canceling commits. It
     * ensures that changes are committed automatically without user prompts.</p>
     *
     * @return {@code true} if auto-commit is enabled; {@code false} otherwise.
     */
    public static boolean isAutoCommitEnabled() {
        return preferences.getBoolean(AUTO_COMMIT_KEY, false);
    }

    /**
     * Updates the user's preference for the auto-commit feature.
     *
     * <p>When auto-commit is enabled, changes are committed automatically without requiring
     * manual intervention, such as editing commit messages or confirming commits. This method
     * allows the caller to enable or disable this behavior.</p>
     *
     * @param enabled {@code true} to enable auto-commit; {@code false} to disable it.
     */
    public static void setAutoCommit(boolean enabled) {
        preferences.putBoolean(AUTO_COMMIT_KEY, enabled);
    }

    /**
     * Checks whether the auto-push feature is enabled.
     *
     * <p>Auto-push, when enabled, automatically pushes committed changes to the remote repository
     * without requiring manual intervention. This feature ensures seamless synchronization
     * between the local and remote repositories.</p>
     *
     * @return {@code true} if auto-push is enabled; {@code false} otherwise.
     */
    public static boolean isAutoPushEnabled() {
        return preferences.getBoolean(AUTO_PUSH_KEY, false);
    }

    /**
     * Updates the user's preference for enabling or disabling the auto-push feature.
     *
     * <p>The auto-push feature, when enabled, ensures that committed changes are automatically
     * pushed to the remote repository without manual intervention. This method allows users
     * to control this behavior by toggling the feature on or off.</p>
     *
     * @param enabled {@code true} to enable auto-push; {@code false} to disable it.
     */
    public static void setAutoPush(boolean enabled) {
        preferences.putBoolean(AUTO_PUSH_KEY, enabled);
    }

    /**
     * Resets the user's preferences for auto-commit and auto-push features.
     *
     * <p>This method removes the stored preferences for the auto-commit and auto-push settings,
     * effectively resetting them to their default state.</p>
     *
     * <p>It does not affect other preferences or settings unrelated to these features.</p>
     */
    public static void reset() {
        preferences.remove(AUTO_COMMIT_KEY);
        preferences.remove(AUTO_PUSH_KEY);
    }

    /**
     * Displays the current user settings for the auto-commit and auto-push features.
     *
     * <p>The method provides a summary of the statuses for these two settings:
     * whether auto-commit and auto-push are enabled or disabled. Additionally, it
     * includes a note regarding the limitations of the auto-commit setting when enabled.</p>
     *
     * @return A formatted string showing the current states of auto-commit and auto-push,
     * along with a note about the behavior of the auto-commit feature.
     */
    public static String displaySettings() {
        return String.format("""
                                     Current Settings:
                                       Auto-commit: %s
                                       Auto-push:   %s
                                     
                                     Note: When auto-commit is enabled, you won't be able to:
                                       - Regenerate the AI message
                                       - Edit the commit message
                                       - Cancel the commit
                                     """,
                             isAutoCommitEnabled() ? "enabled" : "disabled",
                             isAutoPushEnabled() ? "enabled" : "disabled"
        );
    }
}
