package moe.div.moequickgate.repository;

import java.nio.file.Path;
import moe.div.moequickgate.bean.OperationLogEntry;

/**
 * 操作日志的持久化抽象。
 * Persistence abstraction for operation logs.
 */
public interface LogRepository {
    void append(OperationLogEntry entry);

    Path getLogPath();
}
