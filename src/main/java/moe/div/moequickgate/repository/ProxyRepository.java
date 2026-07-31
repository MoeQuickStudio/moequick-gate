package moe.div.moequickgate.repository;

import java.util.List;
import java.util.OptionalLong;
import moe.div.moequickgate.bean.MoeProxy;

/**
 * 代理配置持久化抽象。
 * Persistence abstraction for proxy profiles.
 */
public interface ProxyRepository {
    List<MoeProxy> findAll();

    OptionalLong findSelectedId();

    MoeProxy create(MoeProxy proxy);

    void update(MoeProxy proxy);

    void deleteById(long id);

    void select(long id);
}
