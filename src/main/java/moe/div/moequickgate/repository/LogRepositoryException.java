package moe.div.moequickgate.repository;

/**
 * 操作日志无法初始化或写入。
 * Raised when the operation log cannot be initialized or written.
 */
public final class LogRepositoryException extends RuntimeException {
    public LogRepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
