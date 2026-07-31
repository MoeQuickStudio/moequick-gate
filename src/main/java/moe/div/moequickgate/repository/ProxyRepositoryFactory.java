package moe.div.moequickgate.repository;

import java.nio.file.Path;
import moe.div.moequickgate.database.DatabasePathResolver;
import moe.div.moequickgate.database.SQLiteHelper;

/**
 * 创建默认 Repository，并在数据库不可用时安全降级。
 * Creates the default repository and safely falls back when SQLite is unavailable.
 */
public final class ProxyRepositoryFactory {
    private ProxyRepositoryFactory() {
    }

    public static RepositoryContext createDefault() {
        return create(DatabasePathResolver.resolveDefault());
    }

    public static RepositoryContext create(Path databasePath) {
        try {
            ProxyRepository repository = new SQLiteProxyRepository(new SQLiteHelper(databasePath));
            repository.findAll();
            repository.findSelectedId();
            return new RepositoryContext(repository, true, "", databasePath);
        } catch (RuntimeException exception) {
            String reason = rootMessage(exception);
            String warning = "数据库不可用，当前更改不会保存。路径："
                    + databasePath.toAbsolutePath().normalize() + "；原因：" + reason;
            return new RepositoryContext(
                    InMemoryProxyRepository.seeded(), false, warning, databasePath);
        }
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}
