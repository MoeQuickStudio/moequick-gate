package moe.div.moequickgate.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import moe.div.moequickgate.bean.MoeProxy;
import moe.div.moequickgate.bean.ProxyProtocol;
import moe.div.moequickgate.database.SQLiteHelper;

/**
 * 基于 SQLite 的代理配置 Repository。
 * SQLite-backed proxy profile repository.
 */
public final class SQLiteProxyRepository implements ProxyRepository {
    private final SQLiteHelper database;

    public SQLiteProxyRepository(SQLiteHelper database) {
        this.database = database;
        database.initialize();
    }

    @Override
    public List<MoeProxy> findAll() {
        String sql = "SELECT id, name, host, port, protocol FROM proxy_profile ORDER BY id";
        try (Connection connection = database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) {
            List<MoeProxy> proxies = new ArrayList<>();
            while (result.next()) {
                proxies.add(readProxy(result));
            }
            return proxies;
        } catch (SQLException exception) {
            throw failure("读取代理列表失败", exception);
        }
    }

    @Override
    public OptionalLong findSelectedId() {
        String sql = "SELECT current_proxy_id FROM app_state WHERE singleton_id = 1";
        try (Connection connection = database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) {
            if (!result.next()) {
                return OptionalLong.empty();
            }
            long id = result.getLong(1);
            return result.wasNull() ? OptionalLong.empty() : OptionalLong.of(id);
        } catch (SQLException exception) {
            throw failure("读取当前代理失败", exception);
        }
    }

    @Override
    public MoeProxy create(MoeProxy proxy) {
        String insertSql = "INSERT INTO proxy_profile (name, host, port, protocol) VALUES (?, ?, ?, ?)";
        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                boolean wasEmpty = countProfiles(connection) == 0;
                long id;
                try (PreparedStatement statement = connection.prepareStatement(
                        insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, proxy.getName());
                    statement.setString(2, proxy.getHost());
                    statement.setInt(3, proxy.getPort());
                    statement.setString(4, proxy.getProtocol().name());
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("SQLite 未返回新代理配置 ID");
                        }
                        id = keys.getLong(1);
                    }
                }
                if (wasEmpty) {
                    updateSelection(connection, id);
                }
                connection.commit();
                return new MoeProxy(
                        id, proxy.getName(), proxy.getHost(), proxy.getPort(), proxy.getProtocol());
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw failure("创建代理配置失败", exception);
        }
    }

    @Override
    public void update(MoeProxy proxy) {
        String sql = "UPDATE proxy_profile SET name = ?, host = ?, port = ?, protocol = ? WHERE id = ?";
        try (Connection connection = database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, proxy.getName());
            statement.setString(2, proxy.getHost());
            statement.setInt(3, proxy.getPort());
            statement.setString(4, proxy.getProtocol().name());
            statement.setLong(5, proxy.getId());
            requireChanged(statement.executeUpdate(), proxy.getId());
        } catch (SQLException exception) {
            throw failure("更新代理配置失败", exception);
        }
    }

    @Override
    public void deleteById(long id) {
        String sql = "DELETE FROM proxy_profile WHERE id = ?";
        try (Connection connection = database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            requireChanged(statement.executeUpdate(), id);
        } catch (SQLException exception) {
            throw failure("删除代理配置失败", exception);
        }
    }

    @Override
    public void select(long id) {
        try (Connection connection = database.openConnection()) {
            if (!exists(connection, id)) {
                throw new RepositoryException("代理配置不存在：" + id);
            }
            updateSelection(connection, id);
        } catch (SQLException exception) {
            throw failure("保存当前代理失败", exception);
        }
    }

    private int countProfiles(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM proxy_profile")) {
            return result.next() ? result.getInt(1) : 0;
        }
    }

    private boolean exists(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM proxy_profile WHERE id = ?")) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private void updateSelection(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE app_state SET current_proxy_id = ? WHERE singleton_id = 1")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }

    private MoeProxy readProxy(ResultSet result) throws SQLException {
        return new MoeProxy(
                result.getLong("id"),
                result.getString("name"),
                result.getString("host"),
                result.getInt("port"),
                ProxyProtocol.valueOf(result.getString("protocol")));
    }

    private void requireChanged(int count, long id) {
        if (count == 0) {
            throw new RepositoryException("代理配置不存在：" + id);
        }
    }

    private RepositoryException failure(String action, SQLException exception) {
        return new RepositoryException(action + "：" + database.getDatabasePath(), exception);
    }
}
