package moe.div.moequickgate.proxy;

import java.net.URI;
import java.net.URISyntaxException;
import moe.div.moequickgate.bean.MoeProxy;
import moe.div.moequickgate.bean.ProxyProtocol;

/**
 * 安全构造写入系统配置的代理 URI。
 * Safely builds proxy URIs written to system configuration.
 */
public final class ProxyUriFactory {
    private ProxyUriFactory() {
    }

    public static String create(MoeProxy proxy) {
        if (proxy == null) {
            throw new IllegalArgumentException("尚未选择代理配置。");
        }
        if (proxy.getProtocol() == ProxyProtocol.SOCKS5) {
            throw new IllegalArgumentException("APT 和 NPM 暂不支持 SOCKS5 代理。");
        }

        String host = proxy.getHost();
        if (host == null
                || host.isBlank()
                || host.indexOf('"') >= 0
                || host.indexOf('\'') >= 0
                || host.indexOf(';') >= 0
                || host.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("代理主机包含不能写入系统配置的字符。");
        }

        String scheme = proxy.getProtocol() == ProxyProtocol.HTTPS ? "https" : "http";
        try {
            URI uri = new URI(scheme, null, host, proxy.getPort(), "/", null, null);
            if (uri.getHost() == null) {
                throw new IllegalArgumentException("代理主机不是有效的主机名、IPv4 或 IPv6 地址。");
            }
            return uri.toASCIIString();
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("代理地址无法构成有效 URI。", exception);
        }
    }
}
