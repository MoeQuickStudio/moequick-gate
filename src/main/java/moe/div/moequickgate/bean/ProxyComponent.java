package moe.div.moequickgate.bean;

/**
 * 第一版支持的系统组件。
 * System components supported by the first release.
 */
public enum ProxyComponent {
    APT("APT"),
    NPM("NPM");

    private final String displayName;

    ProxyComponent(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
