package moe.div.moequickgate.repository;

/**
 * Repository 操作失败时抛出的统一异常。
 * Unified exception for repository operation failures.
 */
public final class RepositoryException extends RuntimeException {
    public RepositoryException(String message) {
        super(message);
    }

    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
