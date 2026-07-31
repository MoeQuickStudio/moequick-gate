package moe.div.moequickgate.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import moe.div.moequickgate.bean.MoeProxy;
import moe.div.moequickgate.bean.ProxyProtocol;
import org.junit.jupiter.api.Test;

class ProxyUriFactoryTest {
    @Test
    void buildsHttpHttpsAndIpv6Uris() {
        assertEquals("http://127.0.0.1:7890/", ProxyUriFactory.create(
                new MoeProxy(1, "HTTP", "127.0.0.1", 7890, ProxyProtocol.HTTP)));
        assertEquals("https://[::1]:8443/", ProxyUriFactory.create(
                new MoeProxy(2, "HTTPS", "::1", 8443, ProxyProtocol.HTTPS)));
    }

    @Test
    void rejectsSocksAndConfigurationInjectionCharacters() {
        assertThrows(IllegalArgumentException.class, () -> ProxyUriFactory.create(
                new MoeProxy(1, "SOCKS", "127.0.0.1", 1080, ProxyProtocol.SOCKS5)));
        assertThrows(IllegalArgumentException.class, () -> ProxyUriFactory.create(
                new MoeProxy(2, "Unsafe", "host\";Injected", 7890, ProxyProtocol.HTTP)));
    }
}
