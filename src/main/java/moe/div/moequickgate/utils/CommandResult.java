package moe.div.moequickgate.utils;

import java.time.Duration;

/**
 * 外部命令的完整执行结果。
 * Complete result of an external command execution.
 */
public record CommandResult(
        int exitCode,
        String stdout,
        String stderr,
        Duration duration,
        boolean stdoutTruncated,
        boolean stderrTruncated) {

    public CommandResult {
        stdout = stdout == null ? "" : stdout;
        stderr = stderr == null ? "" : stderr;
        duration = duration == null ? Duration.ZERO : duration;
    }
}
