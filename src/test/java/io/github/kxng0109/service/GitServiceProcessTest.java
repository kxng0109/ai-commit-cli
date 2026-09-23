package io.github.kxng0109.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;

/**
 * Guards for process failure paths in {@link GitService}.
 */
@DisplayName("GitService process failures")
public class GitServiceProcessTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("hung process fails with timeout")
    void hungProcess_failsWithTimeout() {
        GitService service = new GitService(5, tempDir.toString());
        Process process = mock(Process.class);
        when(process.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(process.getErrorStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(process.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        try {
            when(process.waitFor(anyLong(), any(TimeUnit.class))).thenReturn(false);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        when(process.isAlive()).thenReturn(false);

        try (MockedConstruction<ProcessBuilder> ignored = mockConstruction(ProcessBuilder.class,
                (builder, context) -> {
                    when(builder.environment()).thenReturn(new HashMap<>());
                    try {
                        when(builder.start()).thenReturn(process);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })) {
            assertThatThrownBy(() -> invokeRunCommand(service, "git", "diff"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Timeout waiting");
        }

        verify(process, atLeastOnce()).destroyForcibly();
    }

    @Test
    @DisplayName("interrupted wait restores flag and aborts")
    void interruptedWait_restoresFlag() {
        GitService service = new GitService(5, tempDir.toString());
        Process process = mock(Process.class);
        when(process.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(process.getErrorStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(process.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        try {
            when(process.waitFor(anyLong(), any(TimeUnit.class)))
                    .thenThrow(new InterruptedException("interrupted"));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try (MockedConstruction<ProcessBuilder> ignored = mockConstruction(ProcessBuilder.class,
                (builder, context) -> {
                    when(builder.environment()).thenReturn(new HashMap<>());
                    try {
                        when(builder.start()).thenReturn(process);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })) {
            try {
                assertThatThrownBy(() -> invokeRunCommand(service, "git", "diff"))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("interrupted");
                assertThat(Thread.currentThread().isInterrupted()).isTrue();
            } finally {
                Thread.interrupted();
            }
        }
    }

    @Test
    @DisplayName("oversize output aborts with truncation error")
    void oversizeOutput_aborts() {
        GitService service = new GitService(5, tempDir.toString());
        Process process = mock(Process.class);
        when(process.getInputStream())
                .thenReturn(new ByteArrayInputStream(new byte[SecretScanner.MAX_DIFF_BYTES + 1024]));
        when(process.getErrorStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        try {
            when(process.waitFor(anyLong(), any(TimeUnit.class))).thenReturn(true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        when(process.exitValue()).thenReturn(0);

        try (MockedConstruction<ProcessBuilder> ignored = mockConstruction(ProcessBuilder.class,
                (builder, context) -> {
                    when(builder.environment()).thenReturn(new HashMap<>());
                    try {
                        when(builder.start()).thenReturn(process);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })) {
            assertThatThrownBy(() -> invokeRunCommand(service, "git", "diff"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("exceeded");
        }
    }

    @Test
    @DisplayName("missing binary maps to actionable error")
    void missingBinary_mapsToActionableError() {
        GitService service = new GitService(5, tempDir.toString());

        assertThatThrownBy(() -> invokeRunCommand(service, "no-such-binary-xyz-123"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to start");
    }

    @Test
    @DisplayName("failing stream is swallowed by drain")
    void failingStream_swallowedByDrain() throws Exception {
        Method drain = GitService.class.getDeclaredMethod(
                "drainBounded", InputStream.class, StringBuilder.class, AtomicBoolean.class);
        drain.setAccessible(true);
        InputStream failing = mock(InputStream.class);
        when(failing.readNBytes(anyInt())).thenThrow(new IOException("boom"));

        StringBuilder target = new StringBuilder();
        AtomicBoolean truncated = new AtomicBoolean(false);

        drain.invoke(null, failing, target, truncated);

        assertThat(target).isEmpty();
        assertThat(truncated.get()).isFalse();
    }

    @Test
    @DisplayName("close helper tolerates null and failures")
    void closeHelper_toleratesNullAndFailures() throws Exception {
        Method close = GitService.class.getDeclaredMethod("closeQuietly", Closeable.class);
        close.setAccessible(true);

        close.invoke(null, (Closeable) null);

        Closeable failing = mock(Closeable.class);
        doThrow(new IOException("boom")).when(failing).close();
        close.invoke(null, failing);
    }

    @Test
    @DisplayName("command description covers arities")
    void description_coversArities() throws Exception {
        Method describe = GitService.class.getDeclaredMethod("describeCommand", String[].class);
        describe.setAccessible(true);

        assertThat(describe.invoke(null, (Object) new String[]{"git", "commit"})).isEqualTo("git commit");
        assertThat(describe.invoke(null, (Object) new String[]{"git"})).isEqualTo("git");
        assertThat(describe.invoke(null, (Object) new String[0])).isEqualTo("git");
    }

    @Test
    @DisplayName("truncate covers null and overflow")
    void truncate_coversNullAndOverflow() throws Exception {
        Method truncate = GitService.class.getDeclaredMethod("truncate", String.class, int.class);
        truncate.setAccessible(true);

        assertThat(truncate.invoke(null, null, 5)).isEqualTo("");
        assertThat(truncate.invoke(null, "abc", 5)).isEqualTo("abc");
        assertThat((String) truncate.invoke(null, "abcdef", 5)).startsWith("abcde");
    }

    private static void invokeRunCommand(GitService service, String... command) {
        try {
            Method runCommand = GitService.class.getDeclaredMethod("runCommand", String[].class);
            runCommand.setAccessible(true);
            runCommand.invoke(service, (Object) command);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new RuntimeException(cause);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
