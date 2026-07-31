package moe.div.moequickgate.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import moe.div.moequickgate.utils.CommandExecutionException.FailureType;

/**
 * 使用 ProcessBuilder 安全执行参数化命令。
 * Safely executes argument-array commands with ProcessBuilder.
 */
public final class CommandUtil implements CommandExecutor {
    public static final int MAX_OUTPUT_BYTES = 256 * 1024;
    private static final Duration TERMINATION_GRACE = Duration.ofMillis(250);

    @Override
    public CommandResult execute(List<String> command, Duration timeout) {
        List<String> safeCommand = validate(command, timeout);
        long startedAt = System.nanoTime();
        Process process;
        try {
            process = new ProcessBuilder(safeCommand).start();
        } catch (IOException exception) {
            throw new CommandExecutionException(
                    FailureType.START_FAILED,
                    "无法启动命令：" + exception.getMessage(),
                    "",
                    "",
                    exception);
        }

        try (ExecutorService readers = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<CapturedOutput> stdoutFuture =
                    readers.submit(() -> capture(process.getInputStream()));
            Future<CapturedOutput> stderrFuture =
                    readers.submit(() -> capture(process.getErrorStream()));

            boolean finished;
            try {
                finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException exception) {
                terminateTree(process);
                readers.shutdownNow();
                Thread.currentThread().interrupt();
                throw new CommandExecutionException(
                        FailureType.INTERRUPTED,
                        "命令执行被中断。",
                        completedOutput(stdoutFuture),
                        completedOutput(stderrFuture),
                        exception);
            }

            if (!finished) {
                terminateTree(process);
                CapturedOutput stdout = awaitOutput(stdoutFuture, "标准输出");
                CapturedOutput stderr = awaitOutput(stderrFuture, "错误输出");
                throw new CommandExecutionException(
                        FailureType.TIMEOUT,
                        "命令执行超时：" + timeout.toSeconds() + " 秒。",
                        stdout.text(),
                        stderr.text(),
                        null);
            }

            CapturedOutput stdout = awaitOutput(stdoutFuture, "标准输出");
            CapturedOutput stderr = awaitOutput(stderrFuture, "错误输出");
            return new CommandResult(
                    process.exitValue(),
                    stdout.text(),
                    stderr.text(),
                    Duration.ofNanos(System.nanoTime() - startedAt),
                    stdout.truncated(),
                    stderr.truncated());
        }
    }

    private static List<String> validate(List<String> command, Duration timeout) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(timeout, "timeout");
        if (command.isEmpty() || command.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("命令参数不能为空。");
        }
        if (command.getFirst().isBlank()) {
            throw new IllegalArgumentException("命令可执行文件不能为空。");
        }
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("命令超时时间必须大于零。");
        }
        return List.copyOf(command);
    }

    private static CapturedOutput capture(InputStream stream) throws IOException {
        ByteArrayOutputStream retained = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        boolean truncated = false;
        int count;
        while ((count = stream.read(buffer)) != -1) {
            int remaining = MAX_OUTPUT_BYTES - retained.size();
            if (remaining > 0) {
                retained.write(buffer, 0, Math.min(count, remaining));
            }
            if (count > remaining) {
                truncated = true;
            }
        }
        return new CapturedOutput(retained.toString(StandardCharsets.UTF_8), truncated);
    }

    private static CapturedOutput awaitOutput(
            Future<CapturedOutput> future, String streamName) {
        try {
            return future.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CommandExecutionException(
                    FailureType.INTERRUPTED,
                    "读取" + streamName + "时被中断。",
                    "",
                    "",
                    exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            throw new CommandExecutionException(
                    FailureType.OUTPUT_READ_FAILED,
                    "无法读取" + streamName + "：" + rootMessage(cause),
                    "",
                    "",
                    cause);
        }
    }

    private static String completedOutput(Future<CapturedOutput> future) {
        if (!future.isDone() || future.isCancelled()) {
            return "";
        }
        try {
            return future.get().text();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return "";
        } catch (ExecutionException exception) {
            return "";
        }
    }

    private static void terminateTree(Process process) {
        List<ProcessHandle> descendants = new ArrayList<>(process.descendants().toList());
        for (int index = descendants.size() - 1; index >= 0; index--) {
            descendants.get(index).destroy();
        }
        process.destroy();
        try {
            process.waitFor(TERMINATION_GRACE.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
        for (ProcessHandle descendant : descendants) {
            if (descendant.isAlive()) {
                descendant.destroyForcibly();
            }
        }
        if (process.isAlive()) {
            process.destroyForcibly();
        }
    }

    private static String rootMessage(Throwable throwable) {
        if (throwable == null) {
            return "未知错误";
        }
        Throwable current = throwable;
        while ((current.getMessage() == null || current.getMessage().isBlank())
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    private record CapturedOutput(String text, boolean truncated) {
    }
}
