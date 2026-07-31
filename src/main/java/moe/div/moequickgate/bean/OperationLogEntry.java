package moe.div.moequickgate.bean;

import java.time.OffsetDateTime;

/**
 * 一次组件配置变更的操作日志数据。
 * Operation log data for one component configuration change.
 */
public record OperationLogEntry(
        OffsetDateTime timestamp,
        ProxyComponent component,
        Trigger trigger,
        Action action,
        String proxyName,
        String proxyProtocol,
        String proxyEndpoint,
        Result result,
        long durationMillis,
        String detail) {

    public enum Trigger {
        TOGGLE,
        ADD_FIRST,
        SELECT,
        EDIT,
        DELETE
    }

    public enum Action {
        ENABLE,
        DISABLE,
        ROLLBACK_ENABLE,
        ROLLBACK_DISABLE
    }

    public enum Result {
        SUCCESS,
        FAILURE
    }
}
