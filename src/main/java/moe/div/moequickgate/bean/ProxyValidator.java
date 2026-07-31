package moe.div.moequickgate.bean;

import java.util.Objects;

/**
 * 统一代理配置格式校验和标准化。
 * Central proxy profile validation and normalization.
 */
public final class ProxyValidator {
    private static final int MAX_NAME_LENGTH = 64;
    private static final int MAX_HOST_LENGTH = 255;

    private ProxyValidator() {
    }

    public static MoeProxy normalize(
            long id, String name, String host, int port, ProxyProtocol protocol) {
        String normalizedName = name == null ? "" : name.trim();
        String normalizedHost = host == null ? "" : host.trim();

        if (normalizedName.isEmpty() || normalizedName.length() > MAX_NAME_LENGTH) {
            throw new ProxyValidationException("名称长度必须为 1–64 个字符。");
        }
        if (normalizedHost.isEmpty() || normalizedHost.length() > MAX_HOST_LENGTH) {
            throw new ProxyValidationException("主机长度必须为 1–255 个字符。");
        }
        if (normalizedHost.chars().anyMatch(Character::isWhitespace)
                || normalizedHost.contains("://")
                || normalizedHost.contains("/")
                || normalizedHost.contains("\\")) {
            throw new ProxyValidationException("主机不能包含空白、协议前缀或路径。");
        }
        if (port < 1 || port > 65_535) {
            throw new ProxyValidationException("端口必须为 1–65535 的整数。");
        }
        if (protocol == null) {
            throw new ProxyValidationException("请选择代理协议。");
        }

        return new MoeProxy(
                id,
                normalizedName,
                normalizedHost,
                port,
                Objects.requireNonNull(protocol));
    }
}
