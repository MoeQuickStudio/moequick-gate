package moe.div.moequickgate.proxy;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 从组件实际配置中检测到的代理状态。
 * Proxy status detected from the component's real configuration.
 */
public record ProxyRuntimeStatus(boolean available, Map<String, String> routes, String detail) {
    public ProxyRuntimeStatus {
        routes = Map.copyOf(new LinkedHashMap<>(routes == null ? Map.of() : routes));
        detail = detail == null ? "" : detail;
    }

    public static ProxyRuntimeStatus available(Map<String, String> routes) {
        return new ProxyRuntimeStatus(true, routes, "");
    }

    public static ProxyRuntimeStatus unavailable(String detail) {
        return new ProxyRuntimeStatus(false, Map.of(), detail);
    }
}
