package moe.div.moequickgate.database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import moe.div.moequickgate.repository.RepositoryException;

/**
 * 管理 SQLite 连接、初始化和表结构版本。
 * Manages SQLite connections, initialization, and schema versioning.
 */
public final class SQLiteHelper {
    public static final int SCHEMA_VERSION = 1;
    private final Path databasePath;

    public SQLiteHelper(Path databasePath) {
        this.databasePath = databasePath.toAbsolutePath().normalize();
    }

    public Path getDatabasePath() {
        return databasePath;
    }

    public void initialize() {
        createParentDirectory();
        try (Connection connection = openConnection()) {
            int version = readSchemaVersion(connection);
            if (version == 0) {
                createInitialSchema(connection);
            } else if (version > SCHEMA_VERSION) {
                throw new RepositoryException(
                        "数据库版本 " + version + " 高于应用支持的版本 " + SCHEMA_VERSION + "。");
            }
        } catch (SQLException exception) {
            throw failure("初始化数据库失败", exception);
        }
    }

    public Connection openConnection() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
        }
        return connection;
    }

    private void createParentDirectory() {
        try {
            Files.createDirectories(databasePath.getParent());
        } catch (IOException exception) {
            throw new RepositoryException(
                    "无法创建数据库目录：" + databasePath.getParent(), exception);
        }
    }

    private int readSchemaVersion(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("PRAGMA user_version")) {
            return result.next() ? result.getInt(1) : 0;
        }
    }

    private void createInitialSchema(Connection connection) throws SQLException {
        connection.setAutoCommit(false);
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE proxy_profile (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        host TEXT NOT NULL,
                        port INTEGER NOT NULL CHECK (port BETWEEN 1 AND 65535),
                        protocol TEXT NOT NULL CHECK (protocol IN ('HTTP', 'HTTPS', 'SOCKS5'))
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE app_state (
                        singleton_id INTEGER PRIMARY KEY CHECK (singleton_id = 1),
                        current_proxy_id INTEGER,
                        FOREIGN KEY (current_proxy_id) REFERENCES proxy_profile(id) ON DELETE SET NULL
                    )
                    """);
            statement.executeUpdate("""
                    INSERT INTO proxy_profile (name, host, port, protocol)
                    VALUES ('Clash 本机监听', '127.0.0.1', 7890, 'HTTP')
                    """);
            statement.executeUpdate("""
                    INSERT INTO app_state (singleton_id, current_proxy_id)
                    VALUES (1, last_insert_rowid())
                    """);
            statement.execute("PRAGMA user_version = " + SCHEMA_VERSION);
            connection.commit();
        } catch (SQLException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private RepositoryException failure(String action, SQLException exception) {
        return new RepositoryException(action + "：" + databasePath, exception);
    }
}
