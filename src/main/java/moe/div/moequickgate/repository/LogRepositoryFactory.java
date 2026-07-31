package moe.div.moequickgate.repository;

import java.nio.file.Path;
import moe.div.moequickgate.bean.OperationLogEntry;

/**
 * 创建文本日志 Repository，并在不可用时安全降级。
 * Creates the text log repository and safely degrades when unavailable.
 */
public final class LogRepositoryFactory {
    private LogRepositoryFactory() {
    }

    public static LogRepositoryContext createDefault() {
        return create(LogPathResolver.resolveDefault());
    }

    public static LogRepositoryContext create(Path logPath) {
        Path normalized = logPath.toAbsolutePath().normalize();
        try {
            TextLogRepository repository = new TextLogRepository(normalized);
            return new LogRepositoryContext(repository, true, "", normalized);
        } catch (RuntimeException exception) {
            String warning = "操作日志不可用，当前会话不会记录操作。路径："
                    + normalized + "；原因：" + rootMessage(exception);
            return new LogRepositoryContext(
                    new NoOpLogRepository(normalized), false, warning, normalized);
        }
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while ((current.getMessage() == null || current.getMessage().isBlank())
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    private record NoOpLogRepository(Path getLogPath) implements LogRepository {
        @Override
        public void append(OperationLogEntry entry) {
            // 日志不可用时保持业务操作可用。 / Keep business operations available without logging.
        }
    }
}
