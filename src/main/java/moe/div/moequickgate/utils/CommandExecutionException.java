package moe.div.moequickgate.utils;

/**
 * 命令无法正常完成时抛出的结构化异常。
 * Structured exception raised when a command cannot complete normally.
 */
public final class CommandExecutionException extends RuntimeException {
    public enum FailureType {
        START_FAILED,
        TIMEOUT,
        INTERRUPTED,
        OUTPUT_READ_FAILED
    }

    private final FailureType failureType;
    private final String stdout;
    private final String stderr;

    public CommandExecutionException(
            FailureType failureType,
            String message,
            String stdout,
            String stderr,
            Throwable cause) {
        super(message, cause);
        this.failureType = failureType;
        this.stdout = stdout == null ? "" : stdout;
        this.stderr = stderr == null ? "" : stderr;
    }

    public FailureType getFailureType() {
        return failureType;
    }

    public String getStdout() {
        return stdout;
    }

    public String getStderr() {
        return stderr;
    }
}
