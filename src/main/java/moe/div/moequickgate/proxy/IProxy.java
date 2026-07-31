package moe.div.moequickgate.proxy;

import moe.div.moequickgate.bean.MoeProxy;
import moe.div.moequickgate.bean.ProxyComponent;

/**
 * 系统组件代理操作能力。
 * Proxy operations supported by a system component.
 */
public interface IProxy {
    ProxyComponent getComponent();

    ProxyRuntimeStatus check();

    void enable(MoeProxy proxy);

    void disable();
}
