package io.github.kxng0109.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserPreferencesTest {

    @AfterEach
    void cleanup() {
        UserPreferences.reset();
    }

    @Test
    void isAutoCommitEnabled_shouldReturnFalseByDefault() {
        boolean result = UserPreferences.isAutoCommitEnabled();

        assertFalse(result, "Auto-commit should be disabled by default");
    }

    @Test
    void setAutoCommit_withTrue_shouldEnableAutoCommit() {
        UserPreferences.setAutoCommit(true);

        assertTrue(UserPreferences.isAutoCommitEnabled(), "Auto-commit should be enabled");
    }

    @Test
    void setAutoCommit_withFalse_shouldDisableAutoCommit() {
        UserPreferences.setAutoCommit(true);
        UserPreferences.setAutoCommit(false);

        assertFalse(UserPreferences.isAutoCommitEnabled(), "Auto-commit should be disabled");
    }

    @Test
    void isAutoPushEnabled_shouldReturnFalseByDefault() {
        boolean result = UserPreferences.isAutoPushEnabled();

        assertFalse(result, "Auto-push should be disabled by default");
    }

    @Test
    void setAutoPush_withTrue_shouldEnableAutoPush() {
        UserPreferences.setAutoPush(true);

        assertTrue(UserPreferences.isAutoPushEnabled(), "Auto-push should be enabled");
    }

    @Test
    void setAutoPush_withFalse_shouldDisableAutoPush() {
        UserPreferences.setAutoPush(true);
        UserPreferences.setAutoPush(false);

        assertFalse(UserPreferences.isAutoPushEnabled(), "Auto-push should be disabled");
    }

    @Test
    void reset_shouldResetAllPreferencesToDefaults() {
        UserPreferences.setAutoCommit(true);
        UserPreferences.setAutoPush(true);

        UserPreferences.reset();

        assertFalse(UserPreferences.isAutoCommitEnabled(), "Auto-commit should be reset to false");
        assertFalse(UserPreferences.isAutoPushEnabled(), "Auto-push should be reset to false");
    }

    @Test
    void displaySettings_shouldReturnFormattedString() {
        String settings = UserPreferences.displaySettings();

        assertNotNull(settings, "Settings display should not be null");
        assertTrue(settings.contains("Auto-commit:"), "Should contain auto-commit label");
        assertTrue(settings.contains("Auto-push:"), "Should contain auto-push label");
        assertTrue(settings.contains("disabled"), "Should show default disabled state");
    }

    @Test
    void displaySettings_withEnabledSettings_shouldShowEnabled() {
        UserPreferences.setAutoCommit(true);
        UserPreferences.setAutoPush(true);

        String settings = UserPreferences.displaySettings();

        assertTrue(settings.contains("enabled"), "Should show enabled state");
    }

    @Test
    void preferences_shouldPersistAcrossInstances() {
        UserPreferences.setAutoCommit(true);
        UserPreferences.setAutoPush(true);

        boolean autoCommit = UserPreferences.isAutoCommitEnabled();
        boolean autoPush = UserPreferences.isAutoPushEnabled();

        assertTrue(autoCommit, "Auto-commit should persist");
        assertTrue(autoPush, "Auto-push should persist");
    }
}