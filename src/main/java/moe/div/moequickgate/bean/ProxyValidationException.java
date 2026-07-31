package moe.div.moequickgate.bean;

/**
 * 代理配置格式不合法时抛出的异常。
 * Thrown when proxy profile input is invalid.
 */
public final class ProxyValidationException extends IllegalArgumentException {
    public ProxyValidationException(String message) {
        super(message);
    }
}
