package moe.div.moequickgate.proxy;

import moe.div.moequickgate.bean.ProxyComponent;

/**
 * 组件代理操作失败，并携带可展示的处理建议。
 * Component proxy operation failure with an actionable suggestion.
 */
public final class ProxyOperationException extends RuntimeException {
    private final ProxyComponent component;
    private final ProxyFailureType failureType;
    private final String suggestion;
    private final String technicalDetail;

    public ProxyOperationException(
            ProxyComponent component,
            ProxyFailureType failureType,
            String message,
            String suggestion,
            String technicalDetail,
            Throwable cause) {
        super(message, cause);
        this.component = component;
        this.failureType = failureType;
        this.suggestion = suggestion;
        this.technicalDetail = technicalDetail == null ? "" : technicalDetail;
    }

    public ProxyComponent getComponent() {
        return component;
    }

    public ProxyFailureType getFailureType() {
        return failureType;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public String getTechnicalDetail() {
        return technicalDetail;
    }
}
