package moe.div.moequickgate.repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import moe.div.moequickgate.bean.MoeProxy;
import moe.div.moequickgate.bean.ProxyProtocol;

/**
 * 数据库不可用时使用的会话内 Repository。
 * Session-only repository used when the database is unavailable.
 */
public final class InMemoryProxyRepository implements ProxyRepository {
    private final Map<Long, MoeProxy> proxies = new LinkedHashMap<>();
    private long nextId = 1;
    private Long selectedId;

    public static InMemoryProxyRepository seeded() {
        InMemoryProxyRepository repository = new InMemoryProxyRepository();
        repository.create(new MoeProxy(
                0, "Clash 本机监听", "127.0.0.1", 7890, ProxyProtocol.HTTP));
        return repository;
    }

    @Override
    public List<MoeProxy> findAll() {
        return proxies.values().stream().map(MoeProxy::copy).toList();
    }

    @Override
    public OptionalLong findSelectedId() {
        return selectedId == null ? OptionalLong.empty() : OptionalLong.of(selectedId);
    }

    @Override
    public MoeProxy create(MoeProxy proxy) {
        boolean wasEmpty = proxies.isEmpty();
        MoeProxy stored = new MoeProxy(
                nextId++, proxy.getName(), proxy.getHost(), proxy.getPort(), proxy.getProtocol());
        proxies.put(stored.getId(), stored);
        if (wasEmpty) {
            selectedId = stored.getId();
        }
        return stored.copy();
    }

    @Override
    public void update(MoeProxy proxy) {
        requireExisting(proxy.getId());
        proxies.put(proxy.getId(), proxy.copy());
    }

    @Override
    public void deleteById(long id) {
        requireExisting(id);
        proxies.remove(id);
        if (selectedId != null && selectedId == id) {
            selectedId = null;
        }
    }

    @Override
    public void select(long id) {
        requireExisting(id);
        selectedId = id;
    }

    private void requireExisting(long id) {
        if (!proxies.containsKey(id)) {
            throw new RepositoryException("代理配置不存在：" + id);
        }
    }
}
