package moe.div.moequickgate.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CommandUtilTest {
    @TempDir
    Path temporaryDirectory;

    private final CommandUtil commandUtil = new CommandUtil();

    @Test
    void separatesUtf8StdoutAndStderrAndReturnsExitCode() {
        CommandResult streams = commandUtil.execute(command("streams"), Duration.ofSeconds(5));
        CommandResult nonZero = commandUtil.execute(command("exit"), Duration.ofSeconds(5));

        assertEquals(0, streams.exitCode());
        assertEquals("标准输出✓", streams.stdout());
        assertEquals("错误输出✓", streams.stderr());
        assertEquals(7, nonZero.exitCode());
        assertFalse(streams.stdoutTruncated());
        assertFalse(streams.stderrTruncated());
    }

    @Test
    void drainsAndTruncatesLargeOutputWithoutDeadlock() {
        CommandResult result = commandUtil.execute(command("large"), Duration.ofSeconds(10));

        assertEquals(CommandUtil.MAX_OUTPUT_BYTES, result.stdout().length());
        assertEquals(CommandUtil.MAX_OUTPUT_BYTES, result.stderr().length());
        assertTrue(result.stdoutTruncated());
        assertTrue(result.stderrTruncated());
    }

    @Test
    void timesOutAndTerminatesDescendantProcess() throws Exception {
        CommandExecutionException failure = assertThrows(
                CommandExecutionException.class,
                () -> commandUtil.execute(command("spawn-child"), Duration.ofMillis(400)));

        assertEquals(CommandExecutionException.FailureType.TIMEOUT, failure.getFailureType());
        long childPid = Long.parseLong(failure.getStdout().strip().substring("childPid=".length()));
        long deadline = System.nanoTime() + Duration.ofSeconds(2).toNanos();
        while (ProcessHandle.of(childPid).map(ProcessHandle::isAlive).orElse(false)
                && System.nanoTime() < deadline) {
            Thread.sleep(20);
        }
        assertFalse(ProcessHandle.of(childPid).map(ProcessHandle::isAlive).orElse(false));
    }

    @Test
    void interruptionTerminatesProcessAndRestoresInterruptFlag() throws Exception {
        AtomicReference<CommandExecutionException> failure = new AtomicReference<>();
        AtomicBoolean interrupted = new AtomicBoolean();
        Thread thread = Thread.ofPlatform().start(() -> {
            try {
                commandUtil.execute(command("sleep"), Duration.ofSeconds(20));
            } catch (CommandExecutionException exception) {
                failure.set(exception);
                interrupted.set(Thread.currentThread().isInterrupted());
            }
        });

        Thread.sleep(150);
        thread.interrupt();
        thread.join(3000);

        assertFalse(thread.isAlive());
        assertEquals(CommandExecutionException.FailureType.INTERRUPTED,
                failure.get().getFailureType());
        assertTrue(interrupted.get());
    }

    @Test
    void passesMetacharactersLiterallyWithoutShellEvaluation() {
        Path marker = temporaryDirectory.resolve("must-not-exist");
        String literal = "$(touch " + marker + ")";
        CommandResult result =
                commandUtil.execute(command("echo-args", literal), Duration.ofSeconds(5));

        assertEquals(literal, result.stdout());
        assertFalse(marker.toFile().exists());
    }

    @Test
    void validatesCommandAndTimeout() {
        assertThrows(IllegalArgumentException.class,
                () -> commandUtil.execute(List.of(), Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class,
                () -> commandUtil.execute(List.of("java"), Duration.ZERO));
    }

    private static List<String> command(String mode, String... extraArguments) {
        java.util.ArrayList<String> command = new java.util.ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        command.add("-cp");
        command.add("build/classes/java/test");
        command.add(CommandTestHelper.class.getName());
        command.add(mode);
        command.addAll(List.of(extraArguments));
        return command;
    }
}
