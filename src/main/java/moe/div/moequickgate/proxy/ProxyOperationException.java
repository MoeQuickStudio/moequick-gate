package moe.div.moequickgate.proxy;

import moe.div.moequickgate.bean.ProxyComponent;

/**
 * 组件代理操作失败，并携带可展示的处理建议。
 * Component proxy operation failure with an actionable suggestion.
 */
public final class ProxyOperationException extends RuntimeException {
    private final ProxyComponent component;
    private final String suggestion;

    public ProxyOperationException(
            ProxyComponent component, String message, String suggestion) {
        super(message);
        this.component = component;
        this.suggestion = suggestion;
    }

    public ProxyOperationException(
            ProxyComponent component, String message, String suggestion, Throwable cause) {
        super(message, cause);
        this.component = component;
        this.suggestion = suggestion;
    }

    public ProxyComponent getComponent() {
        return component;
    }

    public String getSuggestion() {
        return suggestion;
    }
}
