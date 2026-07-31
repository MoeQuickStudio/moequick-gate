package moe.div.moequickgate.repository;

import java.nio.file.Path;

/**
 * 描述当前日志 Repository 及其可用状态。
 * Describes the active log repository and its availability.
 */
public record LogRepositoryContext(
        LogRepository repository, boolean available, String warning, Path logPath) {
}
