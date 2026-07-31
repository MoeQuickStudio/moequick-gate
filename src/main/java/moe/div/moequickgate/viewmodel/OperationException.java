package moe.div.moequickgate.viewmodel;

/**
 * 跨组件或数据层协调操作失败。
 * Failure while coordinating component and persistence operations.
 */
public final class OperationException extends RuntimeException {
    private final String suggestion;

    public OperationException(String message, String suggestion, Throwable cause) {
        super(message, cause);
        this.suggestion = suggestion;
    }

    public String getSuggestion() {
        return suggestion;
    }
}
