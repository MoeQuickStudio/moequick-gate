package moe.div.moequickgate.repository;

import java.nio.file.Path;

/**
 * 描述当前 Repository 及其持久化能力。
 * Describes the active repository and its persistence capability.
 */
public record RepositoryContext(
        ProxyRepository repository, boolean persistent, String warning, Path databasePath) {
}
