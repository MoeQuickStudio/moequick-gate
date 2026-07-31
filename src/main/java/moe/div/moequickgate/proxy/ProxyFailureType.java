package moe.div.moequickgate.proxy;

/**
 * 组件代理操作的稳定错误分类。
 * Stable failure categories for component proxy operations.
 */
public enum ProxyFailureType {
    TOOL_MISSING,
    INVALID_CONFIGURATION,
    AUTH_CANCELLED,
    PERMISSION_DENIED,
    TIMEOUT,
    PROCESS_FAILED,
    IO_FAILURE,
    INTERRUPTED
}
